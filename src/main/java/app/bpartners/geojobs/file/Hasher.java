package app.bpartners.geojobs.file;

import static app.bpartners.geojobs.file.hash.FileHashAlgorithm.SHA256;

import app.bpartners.geojobs.file.hash.FileHash;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class Hasher implements Function<File, FileHash> {
  private static final ObjectMapper om = new ObjectMapper();

  static {
    om.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
  }

  @Override
  public FileHash apply(File file) {
    try (FileInputStream fis = new FileInputStream(file)) {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = fis.read(buffer)) != -1) {
        digest.update(buffer, 0, bytesRead);
      }

      String base64Checksum = Base64.getEncoder().encodeToString(digest.digest());

      return new FileHash(SHA256, base64Checksum);

    } catch (IOException e) {
      throw new IllegalStateException("Failed to read file: " + file.getPath(), e);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Hash algorithm not supported: SHA-256", e);
    }
  }
}
