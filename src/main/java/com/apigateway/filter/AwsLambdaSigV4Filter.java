package com.apigateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AwsLambdaSigV4Filter implements GlobalFilter, Ordered {

	@Value("${aws.access-key-id}")
	private String accessKeyId;

	@Value("${aws.secret-access-key}")
	private String secretAccessKey;

	@Value("${aws.region:ap-south-1}")
	private String awsRegion;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		String host = request.getURI().getHost();

		// Only sign Lambda Function URL requests
		if (host == null || !host.contains(".lambda-url.")) {
			return chain.filter(exchange);
		}

		// For GET/DELETE (no body)
		HttpMethod method = request.getMethod();
		if (method == HttpMethod.GET || method == HttpMethod.DELETE || method == HttpMethod.HEAD) {
			ServerHttpRequest signedRequest = signRequest(request, new byte[0]);
			return chain.filter(exchange.mutate().request(signedRequest).build());
		}

		// For POST/PUT (with body) - buffer and then sign
		return exchange.getRequest().getBody()
				.collectList()
				.flatMap(dataBuffers -> {
					byte[] body = dataBuffers.stream()
							.map(DataBuffer::asByteBuffer)
							.reduce((b1, b2) -> {
								b1.position(b1.limit());
								b1.limit(b1.capacity());
								b1.put(b2);
								b1.flip();
								return b1;
							})
							.map(b -> {
								byte[] arr = new byte[b.remaining()];
								b.get(arr);
								return arr;
							})
							.orElse(new byte[0]);

					ServerHttpRequest signedRequest = signRequest(request, body);
					return chain.filter(exchange.mutate().request(signedRequest).build());
				});
	}

	private ServerHttpRequest signRequest(ServerHttpRequest request, byte[] body) {
		try {
			URI uri = request.getURI();
			HttpMethod method = request.getMethod();

			// Build headers map from existing request
			Map<String, List<String>> headersMap = new HashMap<>(request.getHeaders());

			String path = uri.getPath() != null ? uri.getPath() : "/";
			String query = uri.getQuery() != null ? uri.getQuery() : "";

			// Create SdkHttpFullRequest for signing
			SdkHttpFullRequest.Builder httpRequestBuilder = SdkHttpFullRequest.builder()
					.method(SdkHttpMethod.fromValue(method.toString()))
					.protocol(uri.getScheme() != null ? uri.getScheme() : "https")
					.host(uri.getHost())
					.port(uri.getPort() == -1 ? 443 : uri.getPort())
					.encodedPath(path)
					.headers(headersMap);

			if (!query.isEmpty()) {
				String[] params = query.split("&");
				for (String param : params) {
					String[] keyValue = param.split("=", 2);
					if (keyValue.length == 2) {
						httpRequestBuilder.appendRawQueryParameter(keyValue[0], keyValue[1]);
					} else if (keyValue.length == 1) {
						httpRequestBuilder.appendRawQueryParameter(keyValue[0], "");
					}
				}
			}

			SdkHttpFullRequest httpRequest = httpRequestBuilder.build();

			// Create credentials
			AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

			// Create signer params
			Aws4SignerParams signerParams = Aws4SignerParams.builder()
					.awsCredentials(credentials)
					.signingName("lambda")
					.signingRegion(Region.of(awsRegion))
					.build();

			// Sign the request
			Aws4Signer signer = Aws4Signer.create();
			SdkHttpFullRequest signedRequest = signer.sign(httpRequest, signerParams);

			// Add signed headers to the request
			ServerHttpRequest.Builder mutatedRequest = request.mutate();

			signedRequest.headers().forEach((key, values) -> {
				mutatedRequest.header(key, values.toArray(new String[0]));
			});

			return mutatedRequest.build();

		} catch (Exception e) {
			System.err.println("SigV4 signing failed: " + e.getMessage());
			e.printStackTrace();
			return request;
		}
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 100;
	}
}