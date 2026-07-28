package org.folio.tcgarage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.PutObjectArgs;
import io.restassured.RestAssured;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class GarageContainerMinioTest {

  @Container
  static GarageContainerMinio garage = new GarageContainerMinio(GarageContainer.PINNED_LATEST);

  @BeforeAll
  static void beforeAll() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @Test
  @SneakyThrows
  void minioClient() {
    var minio = garage.getMinioClient();
    minio.makeBucket(MakeBucketArgs.builder().bucket("sync").build());
    var foo = "foo".getBytes();
    minio.putObject(PutObjectArgs.builder().bucket("sync").object("k").data(foo, foo.length).build());
    var s = minio.getObject(GetObjectArgs.builder().bucket("sync").object("k").build()).readAllBytes();
    assertThat(s, is(foo));
  }

  @Test
  @SneakyThrows
  void minioAsyncClient() {
    var minio = garage.getMinioAsyncClient();
    minio.makeBucket(MakeBucketArgs.builder().bucket("async").build()).get();
    var foo = "foo".getBytes();
    minio.putObject(PutObjectArgs.builder().bucket("async").object("k").data(foo, foo.length).build()).get();
    var s = minio.getObject(GetObjectArgs.builder().bucket("async").object("k").build()).get().readAllBytes();
    assertThat(s, is(foo));
  }

  @Test
  void invalidVersion() {
    try (var gSpace = new GarageContainerMinio("dxflrs/garage: ")) {
      assertThrows(ContainerFetchException.class, gSpace::start);
    }
  }
}
