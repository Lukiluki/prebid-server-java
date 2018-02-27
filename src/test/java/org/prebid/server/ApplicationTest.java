package org.prebid.server;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.Cookie;
import io.restassured.internal.mapping.Jackson2Mapper;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import org.json.JSONException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.prebid.server.model.Uids;
import org.prebid.server.model.request.CookieSyncRequest;
import org.prebid.server.model.response.BidderStatus;
import org.prebid.server.model.response.CookieSyncResponse;
import org.prebid.server.model.response.UsersyncInfo;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(locations = "classpath:org/prebid/server/ApplicationTest/test-application.properties")
public class ApplicationTest extends VertxTest {

    private static final String RUBICON = "rubicon";
    private static final String APPNEXUS = "appnexus";
    private static final String FACEBOOK = "audienceNetwork";
    private static final String PULSEPOINT = "pulsepoint";
    private static final String INDEXEXCHANGE = "indexExchange";
    private static final String LIFESTREET = "lifestreet";
    private static final String PUBMATIC = "pubmatic";
    private static final String CONVERSANT = "conversant";
    private static final String APPNEXUS_ALIAS = "appnexusAlias";
    private static final String CONVERSANT_ALIAS = "conversantAlias";

    private static final int APP_PORT = 8080;
    private static final int WIREMOCK_PORT = 8090;

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(WIREMOCK_PORT);
    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    private static final RequestSpecification spec = new RequestSpecBuilder()
            .setBaseUri("http://localhost")
            .setPort(APP_PORT)
            .setConfig(RestAssuredConfig.config()
                    .objectMapperConfig(new ObjectMapperConfig(new Jackson2Mapper((aClass, s) -> mapper))))
            .build();

    @Test
    public void openrtb2AuctionShouldRespondWithBidsFromDifferentExchanges() throws IOException, JSONException {
        // given
        // rubicon bid response for imp 1
        wireMockRule.stubFor(post(urlPathEqualTo("/rubicon-exchange"))
                .withQueryParam("tk_xint", WireMock.equalTo("rp-pbs"))
                .withBasicAuth("rubicon_user", "rubicon_password")
                .withHeader("Content-Type", equalToIgnoreCase("application/json;charset=utf-8"))
                .withHeader("Accept", WireMock.equalTo("application/json"))
                .withHeader("User-Agent", WireMock.equalTo("prebid-server/1.0"))
                .withRequestBody(equalToJson(jsonFrom("openrtb2/test-rubicon-bid-request-1.json")))
                .willReturn(aResponse().withBody(jsonFrom("openrtb2/test-rubicon-bid-response-1.json"))));

        // rubicon bid response for imp 2
        wireMockRule.stubFor(post(urlPathEqualTo("/rubicon-exchange"))
                .withRequestBody(equalToJson(jsonFrom("openrtb2/test-rubicon-bid-request-2.json")))
                .willReturn(aResponse().withBody(jsonFrom("openrtb2/test-rubicon-bid-response-2.json"))));

        // appnexus bid response for imp 3
        wireMockRule.stubFor(post(urlPathEqualTo("/appnexus-exchange"))
                .withRequestBody(equalToJson(jsonFrom("openrtb2/test-appnexus-bid-request-1.json")))
                .willReturn(aResponse().withBody(jsonFrom("openrtb2/test-appnexus-bid-response-1.json"))));

        // appnexus bid response for imp 3 with alias parameters
        wireMockRule.stubFor(post(urlPathEqualTo("/appnexus-exchange"))
                .withRequestBody(equalToJson(jsonFrom("openrtb2/test-appnexus-bid-request-2.json")))
                .willReturn(aResponse().withBody(jsonFrom("openrtb2/test-appnexus-bid-response-2.json"))));

        // conversant bid response for imp 4
        wireMockRule.stubFor(post(urlPathEqualTo("/conversant-exchange"))
                .withRequestBody(equalToJson(jsonFrom("openrtb2/test-conversant-bid-request-1.json")))
                .willReturn(aResponse().withBody(jsonFrom("openrtb2/test-conversant-bid-response-1.json"))));

        // conversant bid response for imp 4 with alias parameters
        wireMockRule.stubFor(post(urlPathEqualTo("/conversant-exchange"))
                .withRequestBody(equalToJson(jsonFrom("openrtb2/test-conversant-bid-request-2.json")))
                .willReturn(aResponse().withBody(jsonFrom("openrtb2/test-conversant-bid-response-2.json"))));

        // facebook bid response for imp 5
        wireMockRule.stubFor(post(urlPathEqualTo("/facebook-exchange"))
                .withRequestBody(equalToJson(jsonFrom("openrtb2/test-facebook-bid-request-1.json")))
                .willReturn(aResponse().withBody(jsonFrom("openrtb2/test-facebook-bid-response-1.json"))));

        // index bid response for imp 6
        wireMockRule.stubFor(post(urlPathEqualTo("/indexexchange-exchange"))
                .withRequestBody(equalToJson(jsonFrom("openrtb2/test-indexexchange-bid-request-1.json")))
                .willReturn(aResponse().withBody(jsonFrom("openrtb2/test-indexexchange-bid-response-1.json"))));

        // lifestreet bid response for imp 7
        wireMockRule.stubFor(post(urlPathEqualTo("/lifestreet-exchange"))
                .withRequestBody(equalToJson(jsonFrom("openrtb2/test-lifestreet-bid-request-1.json")))
                .willReturn(aResponse().withBody(jsonFrom("openrtb2/test-lifestreet-bid-response-1.json"))));

        // pulsepoint bid response for imp 8
        wireMockRule.stubFor(post(urlPathEqualTo("/pulsepoint-exchange"))
                .withRequestBody(equalToJson(jsonFrom("openrtb2/test-pulsepoint-bid-request-1.json")))
                .willReturn(aResponse().withBody(jsonFrom("openrtb2/test-pulsepoint-bid-response-1.json"))));

        // pubmatic bid response for imp 9
        wireMockRule.stubFor(post(urlPathEqualTo("/pubmatic-exchange"))
                .withRequestBody(equalToJson(jsonFrom("openrtb2/test-pubmatic-bid-request-1.json")))
                .willReturn(aResponse().withBody(jsonFrom("openrtb2/test-pubmatic-bid-response-1.json"))));

        // pre-bid cache
        wireMockRule.stubFor(post(urlPathEqualTo("/cache"))
                .withRequestBody(equalToJson(jsonFrom("openrtb2/test-cache-request.json")))
                .willReturn(aResponse().withBody(jsonFrom("openrtb2/test-cache-response.json"))));

        // when
        final Response response = given(spec)
                .header("Referer", "http://www.example.com")
                .header("X-Forwarded-For", "192.168.244.1")
                .header("User-Agent", "userAgent")
                .header("Origin", "http://www.example.com")
                // this uids cookie value stands for
                // {"uids":{"rubicon":"J5VLCWQP-26-CWFT","adnxs":"12345","audienceNetwork":"FB-UID",
                // "pulsepoint":"PP-UID","indexExchange":"IE-UID","lifestreet":"LS-UID","pubmatic":"PM-UID",
                // "conversant":"CV-UID"}}
                .cookie("uids", "eyJ1aWRzIjp7InJ1Ymljb24iOiJKNVZMQ1dRUC0yNi1DV0ZUIiwiYWRueHMiOiIxMjM0NSIsImF1ZGllbm" +
                        "NlTmV0d29yayI6IkZCLVVJRCIsInB1bHNlcG9pbnQiOiJQUC1VSUQiLCJpbmRleEV4Y2hhbmdlIjoiSUUtVUlEIiwi" +
                        "bGlmZXN0cmVldCI6IkxTLVVJRCIsInB1Ym1hdGljIjoiUE0tVUlEIiwiY29udmVyc2FudCI6IkNWLVVJRCJ9fQ==")
                .body(jsonFrom("openrtb2/test-auction-request.json"))
                .post("/openrtb2/auction");


        // then
        String expectedAuctionResponse = auctionResponseFrom(jsonFrom("openrtb2/test-auction-response.json"),
                response, "ext.responsetimemillis.%s");

        JSONAssert.assertEquals(expectedAuctionResponse, response.asString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    private static String setResponseTime(Response response, String expectedResponseJson, String exchange,
                                          String responseTimePath) {
        final Object val = response.path(format(responseTimePath, exchange));
        final Integer responseTime = val instanceof Integer ? (Integer) val : null;
        if (responseTime != null) {
            expectedResponseJson = expectedResponseJson.replaceAll("\"\\{\\{ " + exchange + "\\.response_time_ms }}\"",
                    responseTime.toString());
        }
        return expectedResponseJson;
    }

    @Test
    public void ampShouldReturnTargeting() throws IOException {
        // given
        // rubicon exchange
        wireMockRule.stubFor(post(urlPathEqualTo("/rubicon-exchange"))
                .withRequestBody(equalToJson(jsonFrom("amp/test-rubicon-bid-request.json")))
                .willReturn(aResponse().withBody(jsonFrom("amp/test-rubicon-bid-response.json"))));

        // pre-bid cache
        wireMockRule.stubFor(post(urlPathEqualTo("/cache"))
                .withRequestBody(equalToJson(jsonFrom("amp/test-cache-request.json")))
                .willReturn(aResponse().withBody(jsonFrom("amp/test-cache-response.json"))));

        // when and then
        given(spec)
                .header("Referer", "http://www.example.com")
                .header("X-Forwarded-For", "192.168.244.1")
                .header("User-Agent", "userAgent")
                .header("Origin", "http://www.example.com")
                // this uids cookie value stands for
                // {"uids":{"rubicon":"J5VLCWQP-26-CWFT"}}
                .cookie("uids", "eyJ1aWRzIjp7InJ1Ymljb24iOiJKNVZMQ1dRUC0yNi1DV0ZUIn19")
                .when()
                .get("/openrtb2/amp?tag_id=test-amp-stored-request")
                .then()
                .assertThat()
                .body(equalTo(jsonFrom("amp/test-amp-response.json")));
    }

    @Test
    public void auctionShouldRespondWithBidsFromDifferentExchanges() throws IOException {
        // given
        // rubicon bid response for ad unit 1
        wireMockRule.stubFor(post(urlPathEqualTo("/rubicon-exchange"))
                .withQueryParam("tk_xint", WireMock.equalTo("rp-pbs"))
                .withBasicAuth("rubicon_user", "rubicon_password")
                .withHeader("Content-Type", equalToIgnoreCase("application/json;charset=utf-8"))
                .withHeader("Accept", WireMock.equalTo("application/json"))
                .withHeader("User-Agent", WireMock.equalTo("prebid-server/1.0"))
                .withRequestBody(equalToJson(jsonFrom("auction/test-rubicon-bid-request-1.json")))
                .willReturn(aResponse().withBody(jsonFrom("auction/test-rubicon-bid-response-1.json"))));

        // rubicon bid response for ad unit 2
        wireMockRule.stubFor(post(urlPathEqualTo("/rubicon-exchange"))
                .withRequestBody(equalToJson(jsonFrom("auction/test-rubicon-bid-request-2.json")))
                .willReturn(aResponse().withBody(jsonFrom("auction/test-rubicon-bid-response-2.json"))));

        // rubicon bid response for ad unit 3
        wireMockRule.stubFor(post(urlPathEqualTo("/rubicon-exchange"))
                .withRequestBody(equalToJson(jsonFrom("auction/test-rubicon-bid-request-3.json")))
                .willReturn(aResponse().withBody(jsonFrom("auction/test-rubicon-bid-response-3.json"))));

        // appnexus bid response for ad unit 4
        wireMockRule.stubFor(post(urlPathEqualTo("/appnexus-exchange"))
                .withRequestBody(equalToJson(jsonFrom("auction/test-appnexus-bid-request-1.json")))
                .willReturn(aResponse().withBody(jsonFrom("auction/test-appnexus-bid-response-1.json"))));

        // facebook bid response for ad unit 5
        wireMockRule.stubFor(post(urlPathEqualTo("/facebook-exchange"))
                .withRequestBody(equalToJson(jsonFrom("auction/test-facebook-bid-request-1.json")))
                .willReturn(aResponse().withBody(jsonFrom("auction/test-facebook-bid-response-1.json"))));

        // pulsepoint bid response for ad unit 6
        wireMockRule.stubFor(post(urlPathEqualTo("/pulsepoint-exchange"))
                .withRequestBody(equalToJson(jsonFrom("auction/test-pulsepoint-bid-request-1.json")))
                .willReturn(aResponse().withBody(jsonFrom("auction/test-pulsepoint-bid-response-1.json"))));

        // pulsepoint bid response for ad unit 7
        wireMockRule.stubFor(post(urlPathEqualTo("/indexexchange-exchange"))
                .withRequestBody(equalToJson(jsonFrom("auction/test-indexexchange-bid-request-1.json")))
                .willReturn(aResponse().withBody(jsonFrom("auction/test-indexexchange-bid-response-1.json"))));

        // pulsepoint bid response for ad unit 8
        wireMockRule.stubFor(post(urlPathEqualTo("/lifestreet-exchange"))
                .withRequestBody(equalToJson(jsonFrom("auction/test-lifestreet-bid-request-1.json")))
                .willReturn(aResponse().withBody(jsonFrom("auction/test-lifestreet-bid-response-1.json"))));

        // pulsepoint bid response for ad unit 9
        wireMockRule.stubFor(post(urlPathEqualTo("/pubmatic-exchange"))
                .withRequestBody(equalToJson(jsonFrom("auction/test-pubmatic-bid-request-1.json")))
                .willReturn(aResponse().withBody(jsonFrom("auction/test-pubmatic-bid-response-1.json"))));

        // pulsepoint bid response for ad unit 10
        wireMockRule.stubFor(post(urlPathEqualTo("/conversant-exchange"))
                .withRequestBody(equalToJson(jsonFrom("auction/test-conversant-bid-request-1.json")))
                .willReturn(aResponse().withBody(jsonFrom("auction/test-conversant-bid-response-1.json"))));

        // pre-bid cache
        wireMockRule.stubFor(post(urlPathEqualTo("/cache"))
                .withRequestBody(equalToJson(jsonFrom("auction/test-cache-request.json")))
                .willReturn(aResponse().withBody(jsonFrom("auction/test-cache-response.json"))));

        // when
        final Response response = given(spec)
                .header("Referer", "http://www.example.com")
                .header("X-Forwarded-For", "192.168.244.1")
                .header("User-Agent", "userAgent")
                .header("Origin", "http://www.example.com")
                // this uids cookie value stands for
                // {"uids":{"rubicon":"J5VLCWQP-26-CWFT","adnxs":"12345","audienceNetwork":"FB-UID",
                // "pulsepoint":"PP-UID","indexExchange":"IE-UID","lifestreet":"LS-UID","pubmatic":"PM-UID",
                // "conversant":"CV-UID"}}
                .cookie("uids",
                        "eyJ1aWRzIjp7InJ1Ymljb24iOiJKNVZMQ1dRUC0yNi1DV0ZUIiwiYWRueHMiOiIxMjM0NSIsImF1ZGllbmNlTmV0d29yayI6IkZCLVVJRCIsInB1bHNlcG9pbnQiOiJQUC1VSUQiLCJpbmRleEV4Y2hhbmdlIjoiSUUtVUlEIiwibGlmZXN0cmVldCI6IkxTLVVJRCIsInB1Ym1hdGljIjoiUE0tVUlEIiwiY29udmVyc2FudCI6IkNWLVVJRCJ9fQ==")
                .queryParam("debug", "1")
                .body(jsonFrom("auction/test-auction-request.json"))
                .post("/auction");

        // then
        assertThat(response.header("Cache-Control")).isEqualTo("no-cache, no-store, must-revalidate");
        assertThat(response.header("Pragma")).isEqualTo("no-cache");
        assertThat(response.header("Expires")).isEqualTo("0");
        assertThat(response.header("Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(response.header("Access-Control-Allow-Origin")).isEqualTo("http://www.example.com");

        final String expectedAuctionResponse = auctionResponseFrom(jsonFrom("auction/test-auction-response.json"),
                response, "bidder_status.find { it.bidder == '%s' }.response_time_ms");

        assertThat(response.asString()).isEqualTo(expectedAuctionResponse);
    }

    @Test
    public void statusShouldReturnHttp200Ok() {
        given(spec)
                .when().get("/status")
                .then().assertThat().statusCode(200);
    }

    @Test
    public void optoutShouldSetOptOutFlagAndRedirectToOptOutUrl() {
        wireMockRule.stubFor(post("/optout")
                .withRequestBody(WireMock.equalTo("secret=abc&response=recaptcha1"))
                .willReturn(aResponse().withBody("{\"success\": true}")));

        final Response response = given(spec)
                .header("Content-Type", "application/x-www-form-urlencoded")
                // this uids cookie value stands for {"uids":{"rubicon":"J5VLCWQP-26-CWFT","adnxs":"12345"}}
                .cookie("uids", "eyJ1aWRzIjp7InJ1Ymljb24iOiJKNVZMQ1dRUC0yNi1DV0ZUIiwiYWRueHMiOiIxMjM0NSJ9fQ==")
                .body("g-recaptcha-response=recaptcha1&optout=1")
                .post("/optout");

        assertThat(response.statusCode()).isEqualTo(301);
        assertThat(response.header("location")).isEqualTo("http://optout/url");

        final Cookie cookie = response.getDetailedCookie("uids");
        assertThat(cookie.getDomain()).isEqualTo("cookie-domain");

        // this uids cookie value stands for {"uids":{},"optout":true}
        final Uids uids = decodeUids(cookie.getValue());
        assertThat(uids.getUids()).isEmpty();
        assertThat(uids.getUidsLegacy()).isEmpty();
        assertThat(uids.getOptout()).isTrue();
    }

    @Test
    public void staticShouldReturnHttp200Ok() {
        given(spec)
                .when()
                .get("/static")
                .then()
                .assertThat()
                .statusCode(200);
    }

    @Test
    public void cookieSyncShouldReturnBidderStatusWithRubiconUsersyncInfo() {
        final CookieSyncResponse cookieSyncResponse = given(spec)
                .body(CookieSyncRequest.of("uuid", singletonList(RUBICON)))
                .when()
                .post("/cookie_sync")
                .then()
                .spec(new ResponseSpecBuilder().setDefaultParser(Parser.JSON).build())
                .extract()
                .as(CookieSyncResponse.class);

        assertThat(cookieSyncResponse).isEqualTo(CookieSyncResponse.of("uuid", "no_cookie",
                singletonList(BidderStatus.builder()
                        .bidder(RUBICON)
                        .noCookie(true)
                        .usersync(UsersyncInfo.of("http://localhost:" + WIREMOCK_PORT + "/cookie", "redirect", false))
                        .build())));
    }

    @Test
    public void setuidShouldUpdateRubiconUidInUidCookie() {
        final Cookie uidsCookie = given(spec)
                // this uids cookie value stands for {"uids":{"rubicon":"J5VLCWQP-26-CWFT","adnxs":"12345"},
                // "bday":"2017-08-15T19:47:59.523908376Z"}
                .cookie("uids", "eyJ1aWRzIjp7InJ1Ymljb24iOiJKNVZMQ1dRUC0yNi1DV0ZUIiwiYWRueHMiOiIxMjM0"
                        + "NSJ9LCJiZGF5IjoiMjAxNy0wOC0xNVQxOTo0Nzo1OS41MjM5MDgzNzZaIn0=")
                // this constant is ok to use as long as it coincides with family name
                .queryParam("bidder", RUBICON)
                .queryParam("uid", "updatedUid")
                .when()
                .get("/setuid")
                .then()
                .extract()
                .detailedCookie("uids");

        assertThat(uidsCookie.getDomain()).isEqualTo("cookie-domain");
        assertThat(uidsCookie.getMaxAge()).isEqualTo(7776000);
        assertThat(uidsCookie.getExpiryDate().toInstant())
                .isCloseTo(Instant.now().plus(90, ChronoUnit.DAYS), within(10, ChronoUnit.SECONDS));

        final Uids uids = decodeUids(uidsCookie.getValue());
        assertThat(uids.getBday()).isEqualTo("2017-08-15T19:47:59.523908376Z"); // should be unchanged
        assertThat(uids.getUidsLegacy()).isEmpty();
        assertThat(uids.getUids().get(RUBICON).getUid()).isEqualTo("updatedUid");
        assertThat(uids.getUids().get(RUBICON).getExpires().toInstant())
                .isCloseTo(Instant.now().plus(14, ChronoUnit.DAYS), within(10, ChronoUnit.SECONDS));
        assertThat(uids.getUids().get("adnxs").getUid()).isEqualTo("12345");
        assertThat(uids.getUids().get("adnxs").getExpires().toInstant())
                .isCloseTo(Instant.now().minus(5, ChronoUnit.MINUTES), within(10, ChronoUnit.SECONDS));
    }

    @Test
    public void optionsRequestShouldRespondWithOriginalPolicyHeaders() {
        final Response response = given(spec)
                .header("Origin", "origin.com")
                .header("Access-Control-Request-Method", "GET")
                .when()
                .options("/");

        assertThat(response.header("Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(response.header("Access-Control-Allow-Origin")).isEqualTo("origin.com");
        assertThat(response.header("Access-Control-Allow-Methods")).contains(asList("HEAD", "OPTIONS", "GET", "POST"));
        assertThat(response.header("Access-Control-Allow-Headers"))
                .isEqualTo("Origin,Accept,X-Requested-With,Content-Type");
    }

    @Test
    public void biddersParamsShouldReturnBidderSchemas() throws IOException {
        given(spec)
                .when()
                .get("/bidders/params")
                .then()
                .assertThat()
                .body(equalTo(jsonFrom("params/test-bidder-params-schemas.json")));
    }

    private String jsonFrom(String file) throws IOException {
        // workaround to clear formatting
        return mapper.writeValueAsString(mapper.readTree(this.getClass().getResourceAsStream(
                this.getClass().getSimpleName() + "/" + file)));
    }

    private static String auctionResponseFrom(String template, Response response, String responseTimePath) {
        final Map<String, String> exchanges = new HashMap<>();
        exchanges.put(RUBICON, "http://localhost:" + WIREMOCK_PORT + "/rubicon-exchange?tk_xint=rp-pbs");
        exchanges.put(APPNEXUS, "http://localhost:" + WIREMOCK_PORT + "/appnexus-exchange");
        exchanges.put(FACEBOOK, "http://localhost:" + WIREMOCK_PORT + "/facebook-exchange");
        exchanges.put(PULSEPOINT, "http://localhost:" + WIREMOCK_PORT + "/pulsepoint-exchange");
        exchanges.put(INDEXEXCHANGE, "http://localhost:" + WIREMOCK_PORT + "/indexexchange-exchange");
        exchanges.put(LIFESTREET, "http://localhost:" + WIREMOCK_PORT + "/lifestreet-exchange");
        exchanges.put(PUBMATIC, "http://localhost:" + WIREMOCK_PORT + "/pubmatic-exchange");
        exchanges.put(CONVERSANT, "http://localhost:" + WIREMOCK_PORT + "/conversant-exchange");

        // inputs for aliases
        exchanges.put(APPNEXUS_ALIAS, null);
        exchanges.put(CONVERSANT_ALIAS, null);

        String result = template.replaceAll("\\{\\{ cache_resource_url }}",
                "http://localhost:" + WIREMOCK_PORT + "/cache?uuid=");

        for (final Map.Entry<String, String> exchangeEntry : exchanges.entrySet()) {
            final String exchange = exchangeEntry.getKey();
            result = result.replaceAll("\\{\\{ " + exchange + "\\.exchange_uri }}", exchangeEntry.getValue());
            result = setResponseTime(response, result, exchange, responseTimePath);
        }

        return result;
    }

    private static Uids decodeUids(String value) {
        return Json.decodeValue(Buffer.buffer(Base64.getUrlDecoder().decode(value)), Uids.class);
    }
}