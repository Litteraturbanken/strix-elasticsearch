package sprakbanken.strix;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.*;
import org.apache.lucene.queries.spans.SpanQuery;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.queries.spans.Spans;
import org.opensearch.common.text.Text;
import org.opensearch.search.fetch.FetchContext;
import org.opensearch.search.fetch.FetchSubPhase;
import org.opensearch.search.fetch.FetchSubPhaseProcessor;
import org.opensearch.search.fetch.subphase.highlight.HighlightField;
import org.opensearch.search.fetch.subphase.highlight.SearchHighlightContext;
import org.opensearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class StrixFetchSubPhaseProcessor implements FetchSubPhaseProcessor {

    private FetchContext context;

    public StrixFetchSubPhaseProcessor(FetchContext fetchContext) {
        this.context = fetchContext;
    }

    @Override
    public void setNextReader(LeafReaderContext readerContext) throws IOException {

    }

    @Override
    public void process(FetchSubPhase.HitContext hitContext) throws IOException {
        int numberOfFragments = -1;

        SearchHighlightContext highlight = this.context.highlight();
        if(highlight == null) {
            return;
        }

        boolean found = false;
        for (SearchHighlightContext.Field field : highlight.fields()) {
            if (field.field().equals("strix")) {
                Map<String, Object> options = field.fieldOptions().options();
                if(options != null) {
                    numberOfFragments = (Integer) options.get("number_of_fragments");
                }
                found = true;
                break;
            }
        }

        if(!found) {
            return;
        }

        List<SpanQuery> spanQueries = getSpanQueriesFromContext(this.context);
        Set<Tuple<Integer, Integer>> allHighlights = new HashSet<>();
        // TODO: advance each span query in turn, to allow for getting highlights in document order
        //       instead of getting all spans for a query before moving to the next
        for(SpanQuery spanQuery : spanQueries) {
            if(numberOfFragments != -1 && allHighlights.size() >= numberOfFragments) {
                break;
            }

            if (spanQuery == null) {
                // TODO: will this ever happen?
                continue;
            }

            try {
                // TODO is it faster to use new IndexSearcher(hitContext.reader()) instead of context.searcher() for createWeight?
                SpanWeight weight = spanQuery.createWeight(this.context.searcher(), ScoreMode.COMPLETE_NO_SCORES, 0);
                Spans spans = weight.getSpans(hitContext.readerContext(), SpanWeight.Postings.POSITIONS);

                if (spans == null) {
                    continue;
                }

                int docId = spans.advance(hitContext.docId());
                if(docId == DocIdSetIterator.NO_MORE_DOCS || docId != hitContext.docId()) {
                    continue;
                }

                while (spans.nextStartPosition() != Spans.NO_MORE_POSITIONS && (numberOfFragments == -1 || allHighlights.size() < numberOfFragments)) {
                    int startPos = spans.startPosition();
                    int endPos = spans.endPosition();
                    boolean addHighlight = true;
                    List<Tuple<Integer, Integer>> toBeRemoved = new ArrayList<>();
                    for(Tuple<Integer, Integer> interval : allHighlights) {
                        int intervalFrom = interval.x;
                        int intervalTo = interval.y;
                        if(startPos >= intervalFrom && endPos <= intervalTo) {
                            addHighlight = false;
                            break;
                        }
                        if(intervalFrom >= startPos && intervalTo <= endPos) {
                            toBeRemoved.add(interval);
                        }
                    }
                    if(addHighlight) {
                        toBeRemoved.forEach(allHighlights::remove);
                        allHighlights.add(new Tuple<>(startPos, endPos));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(allHighlights.size() == 0) {
            return;
        }

        List<Tuple<Integer, Integer>> sortedHighlights = new ArrayList<>(allHighlights);
        sortedHighlights.sort((a, b) -> {
            int compare = Integer.compare(b.y - b.x, a.y - a.x);
            if (compare != 0) {
                return compare;
            } else {
                return Integer.compare(a.x, b.x);
            }
        });

        Text[] something = sortedHighlights.stream().map(span -> new Text(span.x + "-" + span.y)).toArray(Text[]::new);
        Map<String, HighlightField> highlightFields = Collections.singletonMap(String.valueOf(hitContext.docId()), new HighlightField("positions", something));
        hitContext.hit().highlightFields(highlightFields);
    }

    private List<SpanQuery> getSpanQueriesFromContext(FetchContext context) {
        List<Tuple<Integer, SpanQuery>> spanQueries = getSpanQueries(context.query(), 1);
        spanQueries.sort(Comparator.comparingInt(a -> a.x));
        return spanQueries.stream().map(spanQuery -> spanQuery.y).collect(Collectors.toList());
    }

    private List<Tuple<Integer, SpanQuery>> getSpanQueries(Query query, int level) {
        List<Tuple<Integer, SpanQuery>> spanQueries = new ArrayList<>();
        if(query instanceof SpanQuery) {
            spanQueries.add(new Tuple<>(level, (SpanQuery) query));
        } else if(query instanceof BooleanQuery) {
            BooleanQuery booleanQuery = (BooleanQuery) query;
            for(BooleanClause clause : booleanQuery.clauses()) {
                spanQueries.addAll(getSpanQueries(clause.getQuery(), level + 1));
            }
        } else if (query instanceof BoostQuery){
            spanQueries.addAll(getSpanQueries(((BoostQuery) query).getQuery(), level));
        }
        return spanQueries;
    }
}
