package co.com.bancolombia.model;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public interface FileService {

    Mono<Boolean> uploadFile(Path fileContent, String bucketName, String objectKey);
    Mono<Boolean> uploadFluxStream(Flux<ByteBuffer> publisher, String bucketName, String objectKey);
    Mono<Boolean> duplicate(String bucketName, String objectKey);
}
