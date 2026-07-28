package org.folio.tcgarage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Testcontainers
class GarageContainerAwsTest {

  @Container
  static GarageContainerAws garage = new GarageContainerAws(GarageContainer.PINNED_LATEST);

  @BeforeAll
  static void beforeAll() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @Test
  void s3Client() {
    var s3 = garage.getS3Client();
    s3.createBucket(CreateBucketRequest.builder().bucket("buck").build());
    s3.putObject(PutObjectRequest.builder().bucket("buck").key("k").build(), RequestBody.fromString("foo"));
    var s = s3.getObjectAsBytes(GetObjectRequest.builder().bucket("buck").key("k").build()).asUtf8String();
    assertThat(s, is("foo"));
  }
}
