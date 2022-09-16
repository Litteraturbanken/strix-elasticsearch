package sprakbanken.strix;

import org.opensearch.search.fetch.FetchContext;
import org.opensearch.search.fetch.FetchSubPhase;
import org.opensearch.search.fetch.FetchSubPhaseProcessor;
import java.io.IOException;

public class StrixFetchSubPhase implements FetchSubPhase {

    public StrixFetchSubPhase() {
    }

    @Override
    public FetchSubPhaseProcessor getProcessor(FetchContext fetchContext) throws IOException {
        return new StrixFetchSubPhaseProcessor(fetchContext);
    }
}
