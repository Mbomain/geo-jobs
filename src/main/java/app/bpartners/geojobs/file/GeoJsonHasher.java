package app.bpartners.geojobs.file;

import static app.bpartners.geojobs.file.hash.FileHashAlgorithm.SHA256;

import app.bpartners.geojobs.file.hash.FileHash;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class GeoJsonHasher implements Function<File, FileHash> {
  private static final ObjectMapper om = new ObjectMapper();

  static {
    om.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
  }

  @Override
  public FileHash apply(File file) {
    try {
      JsonNode jsonNode = om.readTree(file);
      String normalized = om.writeValueAsString(jsonNode);

      MessageDigest digest = MessageDigest.getInstance(SHA256.name());
      byte[] hashBytes = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));

      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        hexString.append(String.format("%02x", b));
      }

      return new FileHash(SHA256, hexString.toString());

    } catch (IOException e) {
      throw new IllegalStateException("Failed to read or parse GeoJSON file: " + file.getPath(), e);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Hash algorithm not supported: " + SHA256.name(), e);
    }
  }
}
