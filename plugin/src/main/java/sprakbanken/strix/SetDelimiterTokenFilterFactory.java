package sprakbanken.strix;

import org.apache.lucene.analysis.TokenStream;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenFilterFactory;

public class SetDelimiterTokenFilterFactory extends AbstractTokenFilterFactory {

    private String delimiter;

    public SetDelimiterTokenFilterFactory(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, settings);
        this.delimiter = settings.get("delimiter", "\u241F");
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new SetDelimiterTokenFilter(tokenStream, this.delimiter);
    }
}
