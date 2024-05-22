package co.com.bancolombia.s3.operations;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;

import java.nio.ByteBuffer;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MultipartS3 {

    private final S3AsyncClient s3AsyncClient;

    // Flux: Each part must be at least 5 MB in size, except the last part.
    public Mono<Boolean> uploadFluxStream(Flux<ByteBuffer> publisher, String bucketName, String objectKey) {
        return createMultipartUpload(bucketName, objectKey)
                .flatMap(response -> uploadParts(publisher, response.uploadId(), bucketName, objectKey)
                        .flatMap(completedParts -> completeMultipartUpload(completedParts, response.uploadId(), bucketName, objectKey)))
                .map(finalResponse -> finalResponse.sdkHttpResponse().isSuccessful());
    }

    private Mono<CreateMultipartUploadResponse> createMultipartUpload(String bucketName, String objectKey) {
        return Mono.fromFuture(s3AsyncClient.createMultipartUpload(builder -> builder.bucket(bucketName).key(objectKey)));
    }

    private Mono<List<CompletedPart>> uploadParts(Flux<ByteBuffer> publisher, String uploadId, String bucketName, String objectKey) {
        return publisher
                .index()
                .flatMapSequential(tuple -> {
                    long partNumber = tuple.getT1() + 1;
                    return uploadPart(tuple.getT2(), uploadId, (int) partNumber, bucketName, objectKey);
                }, 8)
                // Con concurrencia muy alta se genera timeout ya que aunque se envíen 100 peticiones simultaneas, aws procesa secuencialmente por uploadId
                // Adicional, si hay 100 peticiones esperando, equivale a 500MB en memoria (5MB * 100)
                .collectList();
    }

    private Mono<CompletedPart> uploadPart(ByteBuffer byteBuffer, String uploadId, int partNumber, String bucketName, String objectKey) {
        System.out.println("Uploading part " + partNumber);
        return Mono.fromFuture(s3AsyncClient.uploadPart(builder -> builder
                        .bucket(bucketName)
                        .key(objectKey)
                        .partNumber(partNumber)
                        .uploadId(uploadId)
                        .build(), AsyncRequestBody.fromByteBuffer(byteBuffer)))
                .map(uploadPartResponse -> {
                    System.out.println("Part " + partNumber + " uploaded");
                    return CompletedPart.builder()
                                .partNumber(partNumber)
                                .eTag(uploadPartResponse.eTag())
                                .build();
                });
    }

    private Mono<CompleteMultipartUploadResponse> completeMultipartUpload(
            List<CompletedPart> completedParts, String uploadId, String bucketName, String objectKey) {
        System.out.println("Completed Parts: " + completedParts.size());
        return Mono.fromFuture(s3AsyncClient.completeMultipartUpload(builder -> builder
                .bucket(bucketName)
                .key(objectKey)
                .uploadId(uploadId)
                .multipartUpload(builderMp -> builderMp.parts(completedParts))
                .build()));
    }
}
