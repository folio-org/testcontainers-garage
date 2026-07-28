package org.folio.tcgarage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasLength;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.restassured.RestAssured;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class GarageContainerTest {

  @Container
  static GarageContainer garage = new GarageContainer(GarageContainer.PINNED_LATEST);

  @BeforeAll
  static void beforeAll() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }
  @Test
  void adminWithMetricsToken() {
    RestAssured.get(garage.getAdminUrl() + "/v2/GetNodeInfo?node=self")
    .then()
    .statusCode(403);
  }

  @Test
  void adminWithAdminToken() {
    RestAssured.given()
    .header("Authorization", "Bearer " + garage.getAdminToken())
    .get(garage.getAdminUrl() + "/v2/GetNodeInfo?node=self")
    .then()
    .statusCode(200);
  }

  @Test
  void metricsWithoutToken() {
    RestAssured.given()
    .get(garage.getAdminUrl() + "/metrics")
    .then()
    .statusCode(403);
  }

  @Test
  void metricsWithMetricsToken() {
    RestAssured.given()
    .header("Authorization", "Bearer " + garage.getMetricsToken())
    .get(garage.getAdminUrl() + "/metrics")
    .then()
    .statusCode(200);
  }

  @Test
  @SneakyThrows
  void stringConstructor() {
    var name = GarageContainer.PINNED_LATEST.asCanonicalNameString();
    try (var g = new GarageContainer(name)) {
      g.withUserName("abcdefxyz").withPassword("abcdefghijklmnop").withServices("s3").start();
      var minio = MinioClient.builder().credentials("abcdefxyz", "abcdefghijklmnop")
          .endpoint(g.getS3URL()).region(g.getRegion()).build();
      minio.makeBucket(MakeBucketArgs.builder().bucket("buck").build());
      RestAssured.get("http://" + g.getHost() + ":" + g.getMappedPort(3903) + "/health")
      .then()
      .statusCode(200);
    }
  }

  @Test
  void withServices() {
    try (var g = new GarageContainer(GarageContainer.PINNED_LATEST)) {
      assertThrows(UnsupportedOperationException.class, () -> g.withServices("coffee"));
    }
  }

  @Test
  void rpcSecret() {
    try (var g = new GarageContainer(GarageContainer.PINNED_LATEST)) {
      assertThat(g.getRpcSecret(), hasLength(64));
    }
  }

  @Test
  void getS3URLException() {
    class FaultyGarageContainer extends GarageContainer {
      FaultyGarageContainer() {
        super(GarageContainer.PINNED_LATEST);
      }

      @Override
      public String getHost() {
        return "-";
      }
    }

    try (var g = new FaultyGarageContainer()) {
      assertThrows(IllegalStateException.class, g::getS3URL);
    }
  }

  @Test
  void getEndpointException() {
    class FaultyGarageContainer extends GarageContainer {
      FaultyGarageContainer() {
        super(GarageContainer.PINNED_LATEST);
      }

      @Override
      public String getS3URL() {
        return "]";
      }
    }

    try (var g = new FaultyGarageContainer()) {
      assertThrows(IllegalStateException.class, g::getEndpoint);
    }
  }
}
