package com.github.senocak.orkes.config

import com.github.senocak.orkes.logger
import com.netflix.conductor.client.http.MetadataClient
import com.netflix.conductor.client.http.TaskClient
import com.netflix.conductor.client.http.WorkflowClient
import com.netflix.conductor.client.worker.Worker
import io.orkes.conductor.client.ApiClient
import io.orkes.conductor.client.OrkesClients
import io.orkes.conductor.client.automator.TaskRunnerConfigurer
import io.orkes.conductor.client.http.OrkesMetadataClient
import io.orkes.conductor.client.http.OrkesTaskClient
import io.orkes.conductor.client.http.OrkesWorkflowClient
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.io.HttpClientConnectionManager
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.core5.http.io.SocketConfig
import org.apache.hc.core5.ssl.SSLContexts
import org.apache.hc.core5.ssl.TrustStrategy
import org.apache.hc.core5.util.Timeout
import org.slf4j.Logger
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSession
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatusCode
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext

@Configuration
class AppConfig(
    private val conductorProperties: ConductorProperties
) {
    @Bean
    fun orkesClients(): OrkesClients =
        ApiClient(conductorProperties.url)
            .also { it: ApiClient ->
                it.setWriteTimeout(conductorProperties.timeOut)
                it.setReadTimeout(conductorProperties.timeOut)
                it.setConnectTimeout(conductorProperties.timeOut)
            }
            .run { OrkesClients(this) }

    @Bean
    fun apiClient(): ApiClient =
        ApiClient(conductorProperties.url)
            .also { it: ApiClient ->
                it.setWriteTimeout(conductorProperties.timeOut)
                it.setReadTimeout(conductorProperties.timeOut)
                it.setConnectTimeout(conductorProperties.timeOut)
            }

    @Bean
    fun getTaskClient(apiClient: ApiClient, workerList: List<Worker>): TaskClient {
        //var taskClient = orkesClients().taskClient
        val orkesTaskClient = OrkesTaskClient(apiClient)
        val taskRunnerConfigurer: TaskRunnerConfigurer = TaskRunnerConfigurer.Builder(orkesTaskClient, workerList)
            .withThreadCount(conductorProperties.threadCount)
            .withWorkerNamePrefix("OrkesWorkerNamePrefix")
            .build()
        taskRunnerConfigurer.init()
        return orkesTaskClient
    }

    @Bean
    fun getWorkflowClient(apiClient: ApiClient): WorkflowClient =
        //var workflowClient = orkesClients().workflowClient
        OrkesWorkflowClient(apiClient)

    @Bean
    fun getMetadataClient(apiClient: ApiClient): MetadataClient =
        //var metadataClient = orkesClients().metadataClient
        OrkesMetadataClient(apiClient)

    /**
     * Returns a new instance of the RestTemplate class by passing SSL
     * The RestTemplate class is a convenient class for making RESTful web service calls.
     * This method returns a new instance of the RestTemplate class, allowing clients to make multiple independent
     * web service calls using different RestTemplate instances.
     * @return a new instance of the RestTemplate class.
     */
    @Bean
    @Primary
    fun restTemplateByPassSSL(): RestTemplate {
        val acceptingTrustStrategy = TrustStrategy { x509Certificates: Array<X509Certificate?>?, authType: String? -> true }
        val sslContext: SSLContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build()
        val csf = DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier())
        val requestFactory = HttpComponentsClientHttpRequestFactory()
        val connectionManager: HttpClientConnectionManager = PoolingHttpClientConnectionManagerBuilder.create().setTlsSocketStrategy(csf).build()
        val httpClient: CloseableHttpClient = HttpClients.custom().setConnectionManager(connectionManager).build()
        requestFactory.httpClient = httpClient
        val restTemplate = RestTemplate(requestFactory)
        restTemplate.interceptors = arrayListOf<ClientHttpRequestInterceptor>(LoggingRequestInterceptor())
        return restTemplate
    }

    /**
     * Creates a `RestTemplate` bean configured with SSL trust settings and custom timeouts.
     * This method sets up a `RestTemplate` instance that bypasses SSL verification and applies
     * specific connection, socket, and request timeouts. It also configures a custom HTTP client
     * and adds a logging interceptor for request and response logging.
     *
     * @param restTemplateBuilder A `RestTemplateBuilder` used to build the base `RestTemplate` instance.
     * @return A configured `RestTemplate` instance with SSL trust settings and custom timeouts.
     */
    @Bean(name = ["sslTrustedRestTemplate"])
    fun sslTrustedRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate {
        HttpsURLConnection.setDefaultHostnameVerifier { _: String?, _: SSLSession? -> true }
        val milliseconds = 100
        val restTemplate: RestTemplate = restTemplateBuilder.build()
        // Connect timeout
        val connectionConfig: ConnectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(milliseconds.toLong()))
                .build()
        // Socket timeout
        val socketConfig: SocketConfig = SocketConfig.custom()
                .setSoTimeout(Timeout.ofMilliseconds(milliseconds.toLong()))
                .build()
        // Connection request timeout
        val requestConfig: RequestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(milliseconds.toLong()))
                .build()
        val connectionManager = PoolingHttpClientConnectionManager()
        connectionManager.defaultSocketConfig = socketConfig
        connectionManager.setDefaultConnectionConfig(connectionConfig)
        val httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build()
        val httpComponentsClientHttpRequestFactory = HttpComponentsClientHttpRequestFactory(httpClient)
        httpComponentsClientHttpRequestFactory.setConnectionRequestTimeout(milliseconds)
        httpComponentsClientHttpRequestFactory.setConnectTimeout(milliseconds)
        restTemplate.setRequestFactory(httpComponentsClientHttpRequestFactory)
        restTemplate.interceptors = arrayListOf<ClientHttpRequestInterceptor>(LoggingRequestInterceptor())
        val stringHttpMessageConverter = StringHttpMessageConverter(StandardCharsets.UTF_8)
        stringHttpMessageConverter.setWriteAcceptCharset(false)
        restTemplate.messageConverters.add(index = 0, element = stringHttpMessageConverter)
        return restTemplate
    }

    internal class LoggingRequestInterceptor : ClientHttpRequestInterceptor {
        private val log: Logger by logger()

        override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
            log.info("""
            ===========================request begin================================================
            URI         : ${request.uri}
            Method      : ${request.method}
            Headers     : ${request.headers}
            Request body: ${String(bytes = body, charset = Charsets.UTF_8)}
            ==========================request end================================================
        """)
            val response: ClientHttpResponse = execution.execute(request, body)
            val responseBody: ByteArray = response.body.readBytes()
            log.info("""
            ============================response begin==========================================
            Status code  : ${response.statusCode}
            Status text  : ${response.statusText}
            Headers      : ${response.headers}
            Response body: ${String(bytes = responseBody, charset = Charsets.UTF_8)}
            =======================response end=================================================
        """)
            return CachedClientHttpResponse(response = response, cachedBody = responseBody)
        }

        private class CachedClientHttpResponse(
            private val response: ClientHttpResponse,
            private val cachedBody: ByteArray
        ) : ClientHttpResponse {
            override fun getStatusCode(): HttpStatusCode = response.statusCode
            //override fun getRawStatusCode() = response.rawStatusCode
            override fun getStatusText(): String = response.statusText
            override fun close(): Unit = response.close()
            override fun getHeaders(): HttpHeaders = response.headers
            override fun getBody(): InputStream = ByteArrayInputStream(cachedBody)
        }
    }
}