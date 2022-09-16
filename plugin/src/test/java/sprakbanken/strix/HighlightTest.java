package sprakbanken.strix;
/**
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.*;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.elasticsearch.test.ESIntegTestCase.Scope;
import org.elasticsearch.analysis.common.CommonAnalysisPlugin;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.*;

@Ignore
@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
@ClusterScope(scope = Scope.SUITE, supportsDedicatedMasters = false, numDataNodes = 1)
public class HighlightTest extends ESIntegTestCase {


    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(CommonAnalysisPlugin.class, StrixPlugin.class);
    }

    @Before
    protected void setup() throws Exception {
        XContentBuilder defaultSettings = getSettings();
        XContentBuilder mapping = getMapping();
        client().admin().indices().prepareCreate("test")
                .setSettings(defaultSettings)
                .addMapping("test", mapping)
                .execute().actionGet();

        client().admin().cluster().prepareHealth().setWaitForEvents(Priority.LANGUID).setWaitForGreenStatus().execute().actionGet();
    }

    private void smallIndex() {
        client().prepareIndex("test", "test", "1").setSource("text", "foo").get();
        client().prepareIndex("test", "test", "2").setSource("text", "foo").get();
        refresh();
    }

    private void largeIndex(int numberOfDocs) {
        List<String> bars = new ArrayList<>();
        for(int i = 0; i < 500; i++) {
            bars.add("bar");
        }
        String bar = String.join(" ", bars);
        for(int i = 0; i < numberOfDocs; i++) {
            client().prepareIndex("test", "test", Integer.toString(i)).setSource("text", "foo " + bar + " foo bar").get();
        }
        refresh();
    }

    public void testStrixHighlightingSmallIndex() throws IOException {
        smallIndex();
        SearchHit[] hits = getSearchHits(QueryBuilders.spanTermQuery("text", "foo"));
        assertEquals(2, hits.length);
        for(SearchHit searchHit : hits) {
            Map<String, HighlightField> highlightFields = searchHit.getHighlightFields();
            HighlightField field = highlightFields.get("positions");
            String name = field.getName();
            assertEquals("positions", name);
            Text[] fragments = field.getFragments();
            String string = fragments[0].string();
            assertEquals("0-1", string);
        }
    }

    public void testHighlightingLargeIndex() {
        largeIndex(100000);
        long before = System.currentTimeMillis();
        SearchHit[] hits = getSearchHits(QueryBuilders.spanTermQuery("text", "foo"));
        long after = System.currentTimeMillis();
        System.out.println("Took: " + (after - before) / 1000 + " s");
        assertEquals(9999, hits.length);
        for(SearchHit searchHit : hits) {
            Map<String, HighlightField> highlightFields =  searchHit.getHighlightFields();
            HighlightField positions = highlightFields.get("positions");
            Text[] fragments = positions.getFragments();
            assertEquals(2, fragments.length);

            String first = fragments[0].string();
            assertEquals("0-1", first);

            String second = fragments[1].string();
            assertEquals("501-502", second);
        }
    }

    public void testMultiWordHighlighting() {
        largeIndex(1);
        SpanTermQueryBuilder foo = QueryBuilders.spanTermQuery("text", "foo");
        SpanTermQueryBuilder bar = QueryBuilders.spanTermQuery("text", "bar");
        SearchHit[] hits = getSearchHits(QueryBuilders.spanNearQuery(foo, 0).addClause(bar));
        assertEquals(1, hits.length);

        Map<String, HighlightField> highlightFields = hits[0].getHighlightFields();
        HighlightField positions = highlightFields.get("positions");
        assertNotNull(positions);
        Text[] fragments = positions.getFragments();
        assertEquals(2, fragments.length);
        assertEquals("0-2", fragments[0].string());
        assertEquals("501-503", fragments[1].string());
    }

    public void testSpanQueryAnyTokenZeroMatches() {
        smallIndex();
        SpanTermQueryBuilder foo = QueryBuilders.spanTermQuery("text", "foo");
        SpanQueryAnyTokenBuilder bar = new SpanQueryAnyTokenBuilder("text", 2);
        SearchHit[] hits = getSearchHits(QueryBuilders.spanNearQuery(foo, 0).addClause(bar));
        assertEquals(0, hits.length);
    }

    public void testSpanQueryAnyToken() {
        client().prepareIndex("test", "test", "1").setSource("text", "foo bar lol").get();
        client().prepareIndex("test", "test", "2").setSource("text", "foo lol bar").get();
        refresh();

        SpanQueryBuilder foo = QueryBuilders.spanTermQuery("text", "foo");
        SpanQueryBuilder bar = new SpanQueryAnyTokenBuilder("text", 1);
        SpanQueryBuilder lol = QueryBuilders.spanTermQuery("text", "lol");
        SpanNearQueryBuilder queryBuilder = QueryBuilders.spanNearQuery(foo, 0).addClause(bar).addClause(lol);
        String cool = queryBuilder.toString();
        SearchHit[] hits = getSearchHits(queryBuilder);
        assertEquals(1, hits.length);

        Map<String, HighlightField> highlightFields = hits[0].getHighlightFields();
        HighlightField positions = highlightFields.get("positions");
        assertNotNull(positions);
        Text[] fragments = positions.getFragments();
        assertEquals(1, fragments.length);
        assertEquals("0-2", fragments[0].string());
    }

    private XContentBuilder getMapping() throws IOException {
        XContentBuilder properties = XContentFactory.jsonBuilder().startObject().startObject("test").startObject("properties");
        properties.startObject("text").field("type", "text").field("analyzer", "text_analyzer").endObject();
        return properties.endObject().endObject().endObject();
    }

    private SearchHit[] getSearchHits(QueryBuilder queryBuilder) {
        SearchResponse searchResponse = client().prepareSearch("test").setTypes("test")
                .setQuery(queryBuilder)
                .setSize(9999)
                .highlighter(new HighlightBuilder().field("strix"))
                .execute().actionGet();
        SearchHits hits = searchResponse.getHits();
        return hits.getHits();
    }

    private XContentBuilder getSettings() throws IOException {
        XContentBuilder analysis = XContentFactory.jsonBuilder().startObject().startObject("index").startObject("analysis");
        analysis.startObject("analyzer").startObject("text_analyzer").startArray("filter").endArray().field("type", "custom").field("tokenizer", "whitespace").endObject().endObject();
        return analysis.endObject().endObject().endObject();
    }
}
**/