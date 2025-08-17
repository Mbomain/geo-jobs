package app.bpartners.geojobs.endpoint.rest.mapper;

import java.io.*;
import java.util.UUID;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
public class FileFromMultipartFileMapper implements Function<MultipartFile, File> {

  private static final String PREFIX = "geojson-upload-";
  private static final String SUFFIX = ".geojson";

  @Override
  public File apply(MultipartFile multipartFile) {
    String uuid = UUID.randomUUID().toString();
    File tempFile;

    try {
      tempFile = File.createTempFile(PREFIX + uuid, SUFFIX);
      log.info("Temporary file created for MultipartFile with name={}", tempFile.getName());
      try (InputStream inputStream = multipartFile.getInputStream();
          OutputStream outputStream = new FileOutputStream(tempFile)) {
        inputStream.transferTo(outputStream);
      }
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new UncheckedIOException("Failed to convert MultipartFile to temp File", e);
    }
    return tempFile;
  }
}
