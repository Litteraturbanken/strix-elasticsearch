package sprakbanken.strix;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermStates;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.queries.spans.*;

import java.io.IOException;
import java.util.Map;

public class SpanQueryAnyToken extends SpanQuery {

    private final String field;
    private final int width;

    public SpanQueryAnyToken(String field, int width) {
        this.field = field;
        this.width = width;
    }

    @Override
    public String getField() {
        return field;
    }

    @Override
    public SpanWeight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
        return new SpanQueryAnyToken.SpanGapWeight(searcher, boost);
    }

    @Override
    public String toString(String field) {
        return "SpanGap(" + field + ":" + width + ")";
    }

    private class SpanGapWeight extends SpanWeight {

        SpanGapWeight(IndexSearcher searcher, float boost) throws IOException {
            super(SpanQueryAnyToken.this, searcher, null, boost);
        }

        @Override
        public void extractTermStates(Map<Term, TermStates> contexts) {

        }

        @Override
        public Spans getSpans(LeafReaderContext ctx, Postings requiredPostings) throws IOException {
            return new GapSpans(width);
        }

        @Override
        public boolean isCacheable(LeafReaderContext ctx) {
            return true;
        }

    }

    public void visit(QueryVisitor visitor) {  }

    @Override
    public boolean equals(Object other) {
        return sameClassAs(other) && equalsTo(getClass().cast(other));
    }

    private boolean equalsTo(SpanQueryAnyToken other) {
        return width == other.width && field.equals(other.field);
    }

    @Override
    public int hashCode() {
        int result = classHash();
        result -= 7 * width;
        return result * 15 - field.hashCode();
    }

    static class GapSpans extends Spans {

        int doc = -1;
        int pos = -1;
        final int width;

        GapSpans(int width) {
            this.width = width;
        }

        @Override
        public int nextStartPosition() {
            return ++pos;
        }

        @Override
        public int startPosition() {
            return pos;
        }

        @Override
        public int endPosition() {
            return pos + width;
        }

        @Override
        public int width() {
            return width;
        }

        @Override
        public void collect(SpanCollector collector) {

        }

        @Override
        public int docID() {
            return doc;
        }

        @Override
        public int nextDoc() {
            pos = -1;
            return ++doc;
        }

        @Override
        public int advance(int target) {
            pos = -1;
            return doc = target;
        }

        @Override
        public long cost() {
            return 0;
        }

        @Override
        public float positionsCost() {
            return 0;
        }
    }
}