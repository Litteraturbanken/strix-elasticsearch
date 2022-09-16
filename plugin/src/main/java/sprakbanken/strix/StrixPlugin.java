package sprakbanken.strix;

import org.opensearch.index.analysis.TokenFilterFactory;
import org.opensearch.index.query.QueryParser;
import org.opensearch.indices.analysis.AnalysisModule.AnalysisProvider;
import org.opensearch.plugins.AnalysisPlugin;
import org.opensearch.plugins.Plugin;
import org.opensearch.plugins.SearchPlugin;
import org.opensearch.search.fetch.FetchSubPhase;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StrixPlugin extends Plugin implements AnalysisPlugin, SearchPlugin {

    @Override
    public Map<String, AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        return Collections.singletonMap("set_delimiter_token_filter", SetDelimiterTokenFilterFactory::new);
    }

    @Override
    public List<FetchSubPhase> getFetchSubPhases(SearchPlugin.FetchPhaseConstructionContext context) {
        return Collections.singletonList(new StrixFetchSubPhase());
    }

    @Override
    public List<QuerySpec<?>> getQueries() {
        QueryParser<SpanQueryAnyTokenBuilder> qp = SpanQueryAnyTokenBuilder::fromXContent;
        return Collections.singletonList(new QuerySpec<>(SpanQueryAnyTokenBuilder.NAME, SpanQueryAnyTokenBuilder::new, qp));
    }

}