package org.folio.tcgarage;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Base64;
import org.apache.commons.lang3.ObjectUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.shaded.org.bouncycastle.util.encoders.Hex;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainer for the docker.io/dxflrs/garage image;
 * <a href="https://garagehq.deuxfleurs.fr/">garage</a> is a
 * lightweight S3-compatible distributed object storage service.
 *
 * <p>{@code AbstractGarageContainer} runs a single (non-distributed) node.
 *
 * <p>To ease the migration from testcontainers-localstack's {@code LocalStackContainer}
 * {@code AbstractGarageContainer} provides the methods {@link #withServices(String...)},
 * {@link #getEndpoint()}, {@link #getAccessKey()}, {@link #getSecretKey()}, and {@link #getRegion()},
 * and uses AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY if provided (via {@link #withEnv(String, String)}
 * or similar method).
 *
 * <p>To ease the migration from testcontainers-minio's {@code MinIOContainer}
 * {@code AbstractGarageContainer} provides the methods {@link #withUserName(String)},
 * {@link #withPassword(String)}, {@link #getUserName()}, {@link #getPassword()}, and {@link #getS3URL()},
 * and uses MINIO_ROOT_USER and MINIO_ROOT_PASSWORD if provided (via {@link #withEnv(String, String)}
 * or similar method).
 *
 * <p>For AWS S3 client use {@link AbstractGarageContainerAws} that has methods
 * that return a client or a client builder that is pre-configured for the garage container.
 *
 * <p>For MinIO client and MinIO async client use {@link AbstractGarageContainerMinio} that has methods
 * that return a client or a client builder that is pre-configured for the garage container.
 *
 * @param <T> self-referencing generic, same as {@code SELF} in {@link GenericContainer}.
 * @see GarageContainer
 * @see AbstractGarageContainerAws#getS3Client()
 * @see AbstractGarageContainerAws#getS3ClientBuilder()
 * @see AbstractGarageContainerMinio#getMinioClient()
 * @see AbstractGarageContainerMinio#getMinioClientBuilder()
 * @see AbstractGarageContainerMinio#getMinioAsyncClient()
 * @see AbstractGarageContainerMinio#getMinioAsyncClientBuilder()
 */
@SuppressWarnings("java:S2160")  // no "equals" override, we use GenericContainer's identity "equals"
public abstract class AbstractGarageContainer<T extends AbstractGarageContainer<T>>
    extends GenericContainer<T> {

  private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("dxflrs/garage");

  private static final int GARAGE_S3_PORT = 3900;
  private static final int GARAGE_ADMIN_PORT = 3903;

  private static final String GARAGE_DEFAULT_ACCESS_KEY = "GARAGE_DEFAULT_ACCESS_KEY";
  private static final String GARAGE_DEFAULT_SECRET_KEY = "GARAGE_DEFAULT_SECRET_KEY";
  private static final String DEFAULT_REGION = "DEFAULT_REGION";

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final String rpcSecret = randomHex(32);
  private final String adminToken = randomBase64(32);
  private final String metricsToken = randomBase64(32);

  /**
   * <a href="https://hub.docker.com/layers/dxflrs/garage/v2.3.0">dxflrs/garage v2.3.0 multi-platform index</a>
   * pinned by sha256.
   */
  public static final DockerImageName V2_3_0 = DockerImageName.parse(
      "dxflrs/garage@sha256:866bd13ed2038ba7e7190e840482bc27234c4afaf77be8cfa439ae088c1e4690");

  /**
   * <a href="https://hub.docker.com/layers/dxflrs/garage/v2.3.0">dxflrs/garage v2.3.0 multi-platform index</a>
   * pinned by sha256.
   *
   * <p>The next testcontainers-garage release will bump {@link PINNED_LATEST} to the latest version.
   */
  public static final DockerImageName PINNED_LATEST = V2_3_0;

  /**
   * Construct a Garage container from the dockerImageName.
   *
   * @param dockerImageName the full image name to use
   */
  protected AbstractGarageContainer(final String dockerImageName) {
    this(DockerImageName.parse(dockerImageName));
  }

  /**
   * Construct a Garage container from the dockerImageName.
   *
   * Only ports 3900 (S3 API) and 3903 (Garage Admin API) get exposed by default.
   *
   * @param dockerImageName the full image name to use
   */
  protected AbstractGarageContainer(final DockerImageName dockerImageName) {
    super(dockerImageName);
    dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
    withExposedPorts(GARAGE_S3_PORT, GARAGE_ADMIN_PORT);
    withCommand("/garage", "server", "--single-node", "--default-bucket");
    waitingFor(Wait.forHttp("/health").forPort(GARAGE_ADMIN_PORT));
  }

  /**
   * This no-op method exists to ease the migration from testcontainers-localstack {@code LocalStackContainer}.
   *
   * @param services must be "s3"
   * @return this
   */
  public T withServices(String... services) {
    for (var service : services) {
      if (! "s3".equals(service)) {
        throw new UnsupportedOperationException("Expected s3 but got " + service);
      }
    }
    return self();
  }

  /**
   * Set the username/access key for the S3 API.
   *
   * @param userName S3 API user name
   * @return this
   */
  public AbstractGarageContainer<T> withUserName(String userName) {
    return withEnv(GARAGE_DEFAULT_ACCESS_KEY, userName);
  }

  /**
   * set the password/secret key for the S3 API.
   *
   * @param password S3 API password
   * @return self();
   */
  public AbstractGarageContainer<T> withPassword(String password) {
    return withEnv(GARAGE_DEFAULT_SECRET_KEY, password);
  }


  @Override
  protected void configure() {
    withCopyToContainer(Transferable.of(config()), "/etc/garage.toml");
    getEnvMap().computeIfAbsent(GARAGE_DEFAULT_ACCESS_KEY, x -> buildAccessKey());
    getEnvMap().computeIfAbsent(GARAGE_DEFAULT_SECRET_KEY, x -> buildSecretKey());
    getEnvMap().computeIfAbsent(DEFAULT_REGION, x -> "garage");
    getEnvMap().computeIfAbsent("GARAGE_DEFAULT_BUCKET", x -> "garage");
  }

  private String buildAccessKey() {
    return ObjectUtils.firstNonNull(
        getEnvMap().get("AWS_ACCESS_KEY_ID"),
        getEnvMap().get("MINIO_ROOT_USER"),
        "GK" + randomHex(16));
  }

  private String buildSecretKey() {
    return ObjectUtils.firstNonNull(
        getEnvMap().get("AWS_SECRET_ACCESS_KEY"),
        getEnvMap().get("MINIO_ROOT_PASSWORD"),
        randomHex(32));
  }

  private String config() {
    return """
        metadata_dir = "/tmp/meta"
        data_dir = "/tmp/data"
        db_engine = "sqlite"

        replication_factor = 1

        rpc_bind_addr = "[::]:3901"
        rpc_public_addr = "127.0.0.1:3901"
        rpc_secret = "%s"

        [s3_api]
        s3_region = "garage"
        api_bind_addr = "[::]:3900"
        root_domain = ".s3.garage.localhost"

        [s3_web]
        bind_addr = "[::]:3902"
        root_domain = ".web.garage.localhost"
        index = "index.html"

        [admin]
        api_bind_addr = "[::]:3903"
        admin_token = "%s"
        metrics_token = "%s"
        """.formatted(rpcSecret, adminToken, metricsToken);
  }

  /**
   * n random bytes, hex encoded
   */
  private static String randomHex(int n) {
    var bytes = new byte[n];
    SECURE_RANDOM.nextBytes(bytes);
    return Hex.toHexString(bytes);
  }

  /**
   * n random bytes, base 64 encoded.
   */
  private static String randomBase64(int n) {
    var bytes = new byte[n];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getEncoder().encodeToString(bytes);
  }

  /**
   * Secret for RPC.
   *
   * <p>The RPC port 3901 is not exposed by default.
   *
   * @return secret for RPC
   */
  public String getRpcSecret() {
    return rpcSecret;
  }

  /**
   * Authorization bearer token for admin URL.
   *
   * @return admin token
   */
  public String getAdminToken() {
    return adminToken;
  }

  /**
   * Metrics token to be used as Authorization bearer token for metrics API at
   * {@link getAdminUrl()} + "/metrics".
   *
   * @return metrics token
   */
  public String getMetricsToken() {
    return metricsToken;
  }

  /**
   * Garage admin URL, mapped from port 3903.
   *
   * @return admin url
   */
  public String getAdminUrl() {
    return "http://" + getHost() + ":" + getMappedPort(GARAGE_ADMIN_PORT);
  }

  /**
   * S3 URL using ip address to enforce path-style access.
   *
   * <p>Sample code:
   * <pre><code>MinioClient minioClient = MinioClient.builder()
   *    .endpoint(getS3URL())
   *    .credentials(getUserName(), getPassword())
   *    .region(getRegion())
   *    .build();
   * </code></pre>
   *
   * @return S3 URL
   * @see GarageContainerAws#getS3Client()
   * @see GarageContainerAws#getS3ClientBuilder()
   * @see GarageContainerMinio#getMinioClient()
   * @see GarageContainerMinio#getMinioClientBuilder()
   * @see GarageContainerMinio#getMinioAsyncClient()
   * @see GarageContainerMinio#getMinioAsyncClientBuilder()
   * @see AbstractGarageContainerAws#getS3Client()
   * @see AbstractGarageContainerAws#getS3ClientBuilder()
   * @see AbstractGarageContainerMinio#getMinioClient()
   * @see AbstractGarageContainerMinio#getMinioClientBuilder()
   * @see AbstractGarageContainerMinio#getMinioAsyncClient()
   * @see AbstractGarageContainerMinio#getMinioAsyncClientBuilder()
   */
  public String getS3URL() {
    try {
      var ip = InetAddress.getByName(getHost()).getHostAddress();
      return "http://" + ip + ":" + getMappedPort(GARAGE_S3_PORT);
    } catch (UnknownHostException e) {
      throw new IllegalStateException("building URL fails", e);
    }
  }

  /**
   * Username/access key for the S3 API.
   *
   * <p>The same as {@link #getAccessKey()} to ease migration from testcontainer's {@code MinIOContainer}.
   *
   * <p>Sample code:
   * <pre><code>MinioClient minioClient = MinioClient.builder()
   *    .endpoint(garage.getS3URL())
   *    .credentials(garage.getUserName(), garage.getPassword())
   *    .region(garage.getRegion())
   *    .build();
   * </code></pre>
   *
   * @return user name for S3 API
   * @see GarageContainerAws#getS3Client()
   * @see GarageContainerAws#getS3ClientBuilder()
   * @see GarageContainerMinio#getMinioClient()
   * @see GarageContainerMinio#getMinioClientBuilder()
   * @see GarageContainerMinio#getMinioAsyncClient()
   * @see GarageContainerMinio#getMinioAsyncClientBuilder()
   * @see AbstractGarageContainerAws#getS3Client()
   * @see AbstractGarageContainerAws#getS3ClientBuilder()
   * @see AbstractGarageContainerMinio#getMinioClient()
   * @see AbstractGarageContainerMinio#getMinioClientBuilder()
   * @see AbstractGarageContainerMinio#getMinioAsyncClient()
   * @see AbstractGarageContainerMinio#getMinioAsyncClientBuilder()
   */
  public String getUserName() {
    return getAccessKey();
  }

  /**
   * Password/secret key for the S3 API.
   *
   * <p>The same as {@link #getSecretKey()} to ease migration from testcontainer's {@code MinIOContainer}.
   *
   * <p>Sample code:
   * <pre><code>MinioClient minioClient = MinioClient.builder()
   *    .endpoint(garage.getS3URL())
   *    .credentials(garage.getUserName(), garage.getPassword())
   *    .region(garage.getRegion())
   *    .build();
   * </code></pre>
   *
   * @return password for S3 API
   * @see GarageContainerAws#getS3Client()
   * @see GarageContainerAws#getS3ClientBuilder()
   * @see GarageContainerMinio#getMinioClient()
   * @see GarageContainerMinio#getMinioClientBuilder()
   * @see GarageContainerMinio#getMinioAsyncClient()
   * @see GarageContainerMinio#getMinioAsyncClientBuilder()
   * @see AbstractGarageContainerAws#getS3Client()
   * @see AbstractGarageContainerAws#getS3ClientBuilder()
   * @see AbstractGarageContainerMinio#getMinioClient()
   * @see AbstractGarageContainerMinio#getMinioClientBuilder()
   * @see AbstractGarageContainerMinio#getMinioAsyncClient()
   * @see AbstractGarageContainerMinio#getMinioAsyncClientBuilder()
   */
  public String getPassword() {
    return getSecretKey();
  }

  /**
   * The pre-configured access key for the garage container.
   *
   * <p>It is a random value unless the environment variable
   * {@code GARAGE_DEFAULT_ACCESS_KEY}, {@code AWS_ACCESS_KEY_ID}, or
   * {@code MINIO_ROOT_USER} (provided via {@link #withEnv(String, String)} or similar method)
   * sets it to the desired access key.
   *
   * <p>Use the access key to build an AWS SDK v2 client:
   * <pre><code>S3Client s3 = S3Client.builder()
   *              .endpointOverride(garage.getEndpoint())
   *              .credentialsProvider(StaticCredentialsProvider.create(
   *                  AwsBasicCredentials.create(garage.getAccessKey(), garage.getSecretKey())))
   *              .region(Region.of(garage.getRegion()))
   *              // https://github.com/aws/aws-sdk-java-v2/issues/6387
   *              .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(false).build())
   *              .build();
   * </code></pre>
   *
   * @return access key
   */
  public String getAccessKey() {
    return getEnvMap().get(GARAGE_DEFAULT_ACCESS_KEY);
  }

  /**
   * The pre-configured secret key for the garage container.
   *
   * <p>It is a random value unless {@link #withEnv} sets
   * {@code GARAGE_DEFAULT_SECRET_KEY}, {@code AWS_SECRET_ACCESS_KEY}, or
   * {@code MINIO_ROOT_PASSWORD} to the desired secret key.
   *
   * <p>Use the secret key to build an AWS SDK v2 client:
   * <pre><code>S3Client s3 = S3Client.builder()
   *              .endpointOverride(localstack.getEndpoint())
   *              .credentialsProvider(StaticCredentialsProvider.create(
   *                  AwsBasicCredentials.create(garage.getAccessKey(), garage.getSecretKey())))
   *              .region(Region.of(garage.getRegion()))
   *              // https://github.com/aws/aws-sdk-java-v2/issues/6387
   *              .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(false).build())
   *              .build();
   * </code></pre>
   *
   * @return secret key
   */
  public String getSecretKey() {
    return getEnvMap().get(GARAGE_DEFAULT_SECRET_KEY);
  }

  /**
   * The HTTP S3 endpoint of garage to be used in test code.
   *
   * <p>Pass it into the AWS S3 client using the {@code S3ClientBuilder endpointOverride(URI)} method:
   * <pre><code>S3Client s3 = S3Client.builder()
   *              .endpointOverride(garage.getEndpoint())
   *              .credentialsProvider(StaticCredentialsProvider.create(
   *                  AwsBasicCredentials.create(garage.getAccessKey(), garage.getSecretKey())))
   *              .region(Region.of(garage.getRegion()))
   *              // https://github.com/aws/aws-sdk-java-v2/issues/6387
   *              .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(false).build())
   *              .build();
   * </code></pre>
   *
   * @return S3 endpoint URI
   */
  public URI getEndpoint() {
    try {
      return new URI(getS3URL());
    } catch (URISyntaxException e) {
      throw new IllegalStateException("getEndpoint() fails", e);
    }
  }

  /**
   * The default region of garage.
   *
   * <p>Pass it into the AWS s3 client using the {@code S3ClientBuilder region(Region)} method or the
   * {@code MinioClient.Builder region(String)} method or the {@code MinioAsyncClient.Builder region(String)} method:
   * <pre><code>S3Client s3 = S3Client.builder()
   *    .endpointOverride(garage.getEndpoint())
   *    .credentialsProvider(StaticCredentialsProvider.create(
   *        AwsBasicCredentials.create(garage.getAccessKey(), garage.getSecretKey())))
   *    .region(Region.of(garage.getRegion()))
   *    // https://github.com/aws/aws-sdk-java-v2/issues/6387
   *    .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(false).build())
   *    .build();
   * </code></pre>
   * <pre><code>MinioClient minioClient = MinioClient.builder()
   *    .endpoint(garage.getS3URL())
   *    .credentials(garage.getUserName(), garage.getPassword())
   *    .region(garage.getRegion())
   *    .build();
   * </code></pre>
   * <pre><code>MinioAsyncClient minioAsyncClient = MinioAsyncClient.builder()
   *    .endpoint(garage.getS3URL())
   *    .credentials(garage.getUserName(), garage.getPassword())
   *    .region(garage.getRegion())
   *    .build();
   * </code></pre>
   *
   * @return default region
   */
  public String getRegion() {
    return getEnvMap().get(DEFAULT_REGION);
  }
}
