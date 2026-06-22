package com.anoncircles.graph.integration;

import com.anoncircles.graph.client.DiscussionsClient;
import com.anoncircles.graph.client.GenaiClient;
import com.anoncircles.graph.client.GenaiUnavailableException;
import com.anoncircles.graph.context.GraphContext;
import com.anoncircles.graph.model.AuthResult;
import com.anoncircles.graph.model.Circle;
import com.anoncircles.graph.model.GenericResult;
import com.anoncircles.graph.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GraphQL-layer integration tests using {@link ExecutionGraphQlServiceTester}.
 *
 * <p>Boots the full graph context but swaps the two downstream clients
 * ({@link DiscussionsClient}, {@link GenaiClient}) with Mockito mocks so
 * tests stay hermetic.
 *
 * <p>The {@code WebGraphQlInterceptor} that normally populates
 * {@link GraphContext} only runs for HTTP requests, so this test injects the
 * context directly via {@code configureExecutionInput}.
 *
 * <p>Covers create / join / leave / edit / delete / generateDescription —
 * the user-requested critical path.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(CirclesGraphIntegrationTest.MockClientsConfig.class)
class CirclesGraphIntegrationTest {

    @Autowired DiscussionsClient discussionsClient;
    @Autowired GenaiClient genaiClient;
    @Autowired ExecutionGraphQlService service;

    private GraphQlTester authedTester;
    private GraphQlTester anonTester;
    private static final String TOKEN = makeTestJwt("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.reset(discussionsClient, genaiClient);
        authedTester = ExecutionGraphQlServiceTester.builder(service)
                .configureExecutionInput((info, builder) ->
                        builder.graphQLContext(c -> c.put(GraphContext.KEY,
                                new GraphContext(TOKEN))).build())
                .build();
        anonTester = ExecutionGraphQlServiceTester.builder(service)
                .configureExecutionInput((info, builder) ->
                        builder.graphQLContext(c -> c.put(GraphContext.KEY,
                                new GraphContext(null))).build())
                .build();
    }

    // ===== createCircle =====

    @Test
    void createCircle_forwards_andReturnsCircle() {
        UUID id = UUID.randomUUID();
        when(discussionsClient.createCircle(eq(TOKEN), eq("rust"), eq("safety")))
                .thenReturn(Mono.just(circle(id, "rust", "safety", true, true, 1)));

        authedTester.document("""
                mutation { createCircle(topic: "rust", description: "safety") {
                  id topic description memberCount isAdmin isMember
                } }
                """)
                .execute()
                .path("createCircle.topic").entity(String.class).isEqualTo("rust")
                .path("createCircle.isAdmin").entity(Boolean.class).isEqualTo(true);

        verify(discussionsClient).createCircle(eq(TOKEN), eq("rust"), eq("safety"));
    }

    // ===== joinCircle =====

    @Test
    void joinCircle_returnsUpdatedCircleWithIsMemberTrue() {
        UUID id = UUID.randomUUID();
        when(discussionsClient.joinCircle(eq(TOKEN), eq(id)))
                .thenReturn(Mono.just(circle(id, "t", "d", false, true, 2)));

        authedTester.document("mutation($i: UUID!) { joinCircle(id: $i) { isMember memberCount } }")
                .variable("i", id.toString())
                .execute()
                .path("joinCircle.isMember").entity(Boolean.class).isEqualTo(true)
                .path("joinCircle.memberCount").entity(Integer.class).isEqualTo(2);
    }

    // ===== leaveCircle =====

    @Test
    void leaveCircle_returnsUpdatedCircleWithIsMemberFalse() {
        UUID id = UUID.randomUUID();
        when(discussionsClient.leaveCircle(eq(TOKEN), eq(id)))
                .thenReturn(Mono.just(circle(id, "t", "d", false, false, 1)));

        authedTester.document("mutation($i: UUID!) { leaveCircle(id: $i) { isMember memberCount } }")
                .variable("i", id.toString())
                .execute()
                .path("leaveCircle.isMember").entity(Boolean.class).isEqualTo(false)
                .path("leaveCircle.memberCount").entity(Integer.class).isEqualTo(1);
    }

    // ===== updateCircleDescription =====

    @Test
    void updateCircleDescription_passesNewText() {
        UUID id = UUID.randomUUID();
        when(discussionsClient.updateCircleDescription(eq(TOKEN), eq(id), eq("polished")))
                .thenReturn(Mono.just(circle(id, "t", "polished", true, true, 1)));

        authedTester.document("mutation($i: UUID!) { " +
                        "updateCircleDescription(id: $i, description: \"polished\") { description } }")
                .variable("i", id.toString())
                .execute()
                .path("updateCircleDescription.description").entity(String.class).isEqualTo("polished");
    }

    // ===== deleteCircle =====

    @Test
    void deleteCircle_returnsSuccess() {
        UUID id = UUID.randomUUID();
        when(discussionsClient.deleteCircle(eq(TOKEN), eq(id))).thenReturn(Mono.just(GenericResult.ok()));

        authedTester.document("mutation($i: UUID!) { deleteCircle(id: $i) { success } }")
                .variable("i", id.toString())
                .execute()
                .path("deleteCircle.success").entity(Boolean.class).isEqualTo(true);
    }

    // ===== generateDescription =====

    @Test
    void generateDescription_streamingChunks_resolveToGeneratedUnion() {
        when(genaiClient.generate(eq("rust"), any(), any()))
                .thenReturn(Flux.just("Curious ", "about ", "rust?"));

        authedTester.document("""
                mutation { generateDescription(topic: "rust", mode: FROM_TOPIC) {
                  __typename
                  ... on DescriptionGenerated { text }
                  ... on DescriptionUnavailable { message }
                } }
                """)
                .execute()
                .path("generateDescription.__typename").entity(String.class).isEqualTo("DescriptionGenerated")
                .path("generateDescription.text").entity(String.class).isEqualTo("Curious about rust?");
    }

    @Test
    void generateDescription_unavailable_resolvesToUnavailableUnion() {
        when(genaiClient.generate(eq("rust"), any(), any()))
                .thenReturn(Flux.error(new GenaiUnavailableException("quota exceeded")));

        authedTester.document("""
                mutation { generateDescription(topic: "rust", mode: FROM_TOPIC) {
                  __typename
                  ... on DescriptionUnavailable { message }
                } }
                """)
                .execute()
                .path("generateDescription.__typename").entity(String.class).isEqualTo("DescriptionUnavailable")
                .path("generateDescription.message").entity(String.class).isEqualTo("quota exceeded");
    }

    @Test
    void generateDescriptionStream_streamsTokens() {
        when(genaiClient.generate(eq("rust"), any(), any()))
                .thenReturn(Flux.just("Curious ", "about ", "rust?"));

        java.util.List<String> collected = authedTester.document("""
                subscription { generateDescriptionStream(topic: "rust", mode: FROM_TOPIC) }
                """)
                .executeSubscription()
                .toFlux("generateDescriptionStream", String.class)
                .collectList()
                .block();

        assertThat(collected).containsExactly("Curious ", "about ", "rust?");
    }

    @Test
    void generateDescriptionStream_upstreamFailure_emitsUNAVAILABLE_error() {
        when(genaiClient.generate(eq("rust"), any(), any()))
                .thenReturn(Flux.error(new GenaiUnavailableException("quota exceeded")));

        Throwable err = catchThrowable(() -> authedTester.document("""
                subscription { generateDescriptionStream(topic: "rust", mode: FROM_TOPIC) }
                """)
                .executeSubscription()
                .toFlux("generateDescriptionStream", String.class)
                .collectList()
                .block());

        assertThat(err).isNotNull();
    }

    @Test
    void unauthenticated_callerSees_UNAUTHENTICATED_extension() {
        anonTester.document("mutation { deleteCircle(id: \"" + UUID.randomUUID() + "\") { success } }")
                .execute()
                .errors()
                .satisfy(errs -> {
                    assertThat(errs).hasSizeGreaterThanOrEqualTo(1);
                    assertThat(errs.get(0).getExtensions()).containsEntry("code", "UNAUTHENTICATED");
                });
    }

    // ===== helpers =====

    private static Circle circle(UUID id, String topic, String description,
                                 boolean isAdmin, boolean isMember, int memberCount) {
        return new Circle(id, topic, description, memberCount, UUID.randomUUID(),
                isAdmin, isMember, OffsetDateTime.parse("2024-01-01T00:00:00Z"));
    }

    @SuppressWarnings("unused")
    private static AuthResult sampleAuth() {
        return new AuthResult("jwt.value",
                new User(UUID.randomUUID(), "Foo-01", OffsetDateTime.parse("2024-01-01T00:00:00Z")));
    }

    /** Builds a fake unsigned JWT with the given sub claim — accepted by the rate limiter. */
    private static String makeTestJwt(String sub) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"" + sub + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".unused";
    }

    @TestConfiguration
    static class MockClientsConfig {
        @Bean @Primary DiscussionsClient discussionsClient() { return mock(DiscussionsClient.class); }
        @Bean @Primary GenaiClient genaiClient() { return mock(GenaiClient.class); }
    }
}
