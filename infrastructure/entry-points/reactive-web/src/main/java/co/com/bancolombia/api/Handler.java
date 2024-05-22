package co.com.bancolombia.api;

import co.com.bancolombia.model.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
public class Handler {

    private final FileService fileService;

    public Mono<ServerResponse> listenUploadFile(ServerRequest serverRequest) {
        return fileService.uploadFile(Paths.get("/Users/johncarm/Downloads/testFile"), "test-saldos-ria", "holafile")
                .flatMap(response -> ServerResponse.ok().bodyValue("UploadFile"));
    }

    public Mono<ServerResponse> listenMultipart(ServerRequest serverRequest) {
        var flux = Flux.range(0, 10_000_000).map(String::valueOf)
                .buffer(1_000_000)
                .map(list -> ByteBuffer.wrap(String.join("\n", list).getBytes()));

        return fileService.uploadFluxStream(flux, "test-saldos-ria", "holamp")
                .flatMap(response -> ServerResponse.ok().bodyValue("Multipart"));
    }

    public Mono<ServerResponse> listenDuplicate(ServerRequest serverRequest) {
        return fileService.duplicate("test-saldos-ria", "holamp")
                .flatMap(response -> ServerResponse.ok().bodyValue(response));
    }

    public Mono<ServerResponse> listenMultiparFormData(ServerRequest serverRequest) {
        return serverRequest.multipartData()
                .flatMap(multiValueMap -> {
                    FilePart part = (FilePart) multiValueMap.toSingleValueMap().get("file"); // Manejar el null
                    String filename = part.filename();
                    Flux<ByteBuffer> flux = getFlux(part);

                    return fileService.uploadFluxStream(flux, "test-saldos-ria", filename)
                            .flatMap(response -> ServerResponse.ok().bodyValue(response));
                });
    }

    private static Flux<ByteBuffer> getFlux(FilePart part) {
        return part
                .content()
                .buffer(1024 * 5) // 1024 (DataBuffer) * 1024 * 5
                .map(dataBuffers -> {
                    int size = dataBuffers.stream().mapToInt(DataBuffer::readableByteCount).sum();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(size);
                    dataBuffers.forEach(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        byteBuffer.put(ByteBuffer.wrap(bytes));

                        //byteBuffer.put(dataBuffer.asByteBuffer()); //Deprecated
                        DataBufferUtils.release(dataBuffer);
                    });
                    return byteBuffer;
                });
    }
}
