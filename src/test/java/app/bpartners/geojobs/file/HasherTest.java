package app.bpartners.geojobs.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;

class HasherTest {

  private final Hasher subject = new Hasher();

  @Test
  void should_have_same_file_hash() throws URISyntaxException {
    var resource = getClass().getResource("/geojson/boulevard-saint-benard.geojson");
    assertNotNull(resource);

    File subjectFile = new File(resource.toURI());
    String expected = subject.apply(subjectFile).value(); // first try
    String actual = subject.apply(subjectFile).value(); // second try

    assertEquals(expected, actual);
  }

  @Test
  void should_have_the_same_hash() throws URISyntaxException {
    var fGeoJR = getClass().getResource("/geojson/anosy-rond-point.geojson");
    var sGeoJR = getClass().getResource("/geojson/ambohimanjaka.geojson");
    assertNotNull(fGeoJR);
    assertNotNull(sGeoJR);

    var fGeoJSON = new File(fGeoJR.toURI());
    var sGeoJSON = new File(sGeoJR.toURI());

    String fHash = subject.apply(fGeoJSON).value();
    String sHash = subject.apply(sGeoJSON).value();

    assertNotEquals(fHash, sHash);
  }
}
