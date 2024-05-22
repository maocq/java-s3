package co.com.bancolombia.s3.operations;

import co.com.bancolombia.model.FileService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.ByteBuffer;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class S3Adapter implements FileService {

    private final S3AsyncClient s3AsyncClient;
    private final MultipartS3 multipartS3;

    @Override
    public Mono<Boolean> uploadFile(Path fileContent, String bucketName, String objectKey) {
        return Mono.fromFuture(s3AsyncClient.putObject(builder -> builder
                .bucket(bucketName)
                .key(objectKey).build(), AsyncRequestBody.fromFile(fileContent)))
                .map(response -> response.sdkHttpResponse().isSuccessful());
    }

    @Override
    public Mono<Boolean> uploadFluxStream(Flux<ByteBuffer> publisher, String bucketName, String objectKey) {
        return multipartS3.uploadFluxStream(publisher, bucketName, objectKey);
    }

    @Override
    public Mono<Boolean> duplicate(String bucketName, String objectKey) { //Fines demostrativos
        return get(bucketName, objectKey)
                .flatMap(source -> move(Flux.from(source), source.response().contentLength(), bucketName, objectKey))
                .map(response -> response.sdkHttpResponse().isSuccessful());
    }

    private Mono<ResponsePublisher<GetObjectResponse>> get(String bucketName, String objectKey) {
        return Mono.fromFuture(s3AsyncClient.getObject(builder -> builder
                .bucket(bucketName)
                .key(objectKey).build(), AsyncResponseTransformer.toPublisher()));
    }

    private Mono<PutObjectResponse> move(Flux<ByteBuffer> publisher, long contentLength, String bucketName, String objectKey) {
        return Mono.fromFuture(s3AsyncClient.putObject(builder -> builder
                        .bucket(bucketName)
                        .key(objectKey + "-copy")
                        .contentLength(contentLength).build(), AsyncRequestBody.fromPublisher(publisher)));
    }
}
