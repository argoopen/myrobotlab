package org.myrobotlab.opencv;

import static org.junit.Assert.assertNotNull;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;

public class OpenCVFilterPyramidDownTest extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = false;
  }

  @Override
  public OpenCVFilter createFilter() {
    // Just to exercise the null and the default constructor.
    // This shouldn't blow up
    OpenCVFilter f = new OpenCVFilterPyramidDown();
    assertNotNull(f.name);
    f.release();
    // Ok, return the named constructor one.
    return new OpenCVFilterPyramidDown("filter");
  }

  @Override
  public IplImage createTestImage() {
    return defaultImage();
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    // Make sure we found 5 faces.
    log.info("CVData: {}", filter.data);
    assertNotNull(output);
    // waitOnAnyKey();
  }

}
