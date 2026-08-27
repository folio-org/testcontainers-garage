package org.folio.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.folio.tcgarage.AbstractGarageContainer;
import org.folio.tcgarage.AbstractGarageContainerAws;
import org.folio.tcgarage.GarageContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

class AbstractTest {
  public class MyGarageContainer extends AbstractGarageContainer<MyGarageContainer> {
    protected MyGarageContainer(DockerImageName dockerImageName) {
      super(dockerImageName);
    }
  }

  @Test
  void myGarageContainer() {
    try (var g = new MyGarageContainer(GarageContainer.PINNED_LATEST)
        .withServices("s3").withUserName("my").withPassword("sec")) {
      assertThat(g.getUserName(), is("my"));
      assertThat(g.getPassword(), is("sec"));
    }
  }

  public class MyGarageContainerAws extends AbstractGarageContainerAws<MyGarageContainerAws> {
    protected MyGarageContainerAws(DockerImageName dockerImageName) {
      super(dockerImageName);
    }
  }

  @Test
  void myGarageContainerAws() {
    try (var g = new MyGarageContainerAws(GarageContainer.PINNED_LATEST)
        .withServices("s3").withUserName("myAws").withPassword("secAws")) {
      assertThat(g.getUserName(), is("myAws"));
      assertThat(g.getPassword(), is("secAws"));
    }
  }

  public class MyGarageContainerMinio extends AbstractGarageContainerAws<MyGarageContainerMinio> {
    protected MyGarageContainerMinio(DockerImageName dockerImageName) {
      super(dockerImageName);
    }
  }

  @Test
  void myGarageContainerMinio() {
    try (var g = new MyGarageContainerMinio(GarageContainer.PINNED_LATEST)
        .withServices("s3").withUserName("myMinio").withPassword("secMinio")) {
      assertThat(g.getUserName(), is("myMinio"));
      assertThat(g.getPassword(), is("secMinio"));
    }
  }
}
