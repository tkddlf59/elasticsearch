/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.search;

import org.apache.lucene.search.BooleanQuery;
import org.elasticsearch.common.NamedRegistry;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.geo.ShapesAvailability;
import org.elasticsearch.common.geo.builders.ShapeBuilders;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.io.stream.NamedWriteable;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ParseFieldRegistry;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.BoostingQueryBuilder;
import org.elasticsearch.index.query.CommonTermsQueryBuilder;
import org.elasticsearch.index.query.ConstantScoreQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.FieldMaskingSpanQueryBuilder;
import org.elasticsearch.index.query.FuzzyQueryBuilder;
import org.elasticsearch.index.query.GeoBoundingBoxQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceRangeQueryBuilder;
import org.elasticsearch.index.query.GeoPolygonQueryBuilder;
import org.elasticsearch.index.query.GeoShapeQueryBuilder;
import org.elasticsearch.index.query.GeohashCellQuery;
import org.elasticsearch.index.query.HasChildQueryBuilder;
import org.elasticsearch.index.query.HasParentQueryBuilder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.IndicesQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.MatchNoneQueryBuilder;
import org.elasticsearch.index.query.MatchPhrasePrefixQueryBuilder;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.ParentIdQueryBuilder;
import org.elasticsearch.index.query.PrefixQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryParser;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.RegexpQueryBuilder;
import org.elasticsearch.index.query.ScriptQueryBuilder;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.index.query.SpanContainingQueryBuilder;
import org.elasticsearch.index.query.SpanFirstQueryBuilder;
import org.elasticsearch.index.query.SpanMultiTermQueryBuilder;
import org.elasticsearch.index.query.SpanNearQueryBuilder;
import org.elasticsearch.index.query.SpanNotQueryBuilder;
import org.elasticsearch.index.query.SpanOrQueryBuilder;
import org.elasticsearch.index.query.SpanTermQueryBuilder;
import org.elasticsearch.index.query.SpanWithinQueryBuilder;
import org.elasticsearch.index.query.TemplateQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.index.query.TypeQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.index.query.WrapperQueryBuilder;
import org.elasticsearch.index.query.functionscore.ExponentialDecayFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.GaussDecayFunctionBuilder;
import org.elasticsearch.index.query.functionscore.LinearDecayFunctionBuilder;
import org.elasticsearch.index.query.functionscore.RandomScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionParser;
import org.elasticsearch.index.query.functionscore.ScriptScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.WeightBuilder;
import org.elasticsearch.indices.query.IndicesQueriesRegistry;
import org.elasticsearch.plugins.SearchPlugin;
import org.elasticsearch.plugins.SearchPlugin.FetchPhaseConstructionContext;
import org.elasticsearch.plugins.SearchPlugin.ScoreFunctionSpec;
import org.elasticsearch.plugins.SearchPlugin.SearchPluginSpec;
import org.elasticsearch.search.action.SearchTransportService;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregator;
import org.elasticsearch.search.aggregations.Aggregator.Parser;
import org.elasticsearch.search.aggregations.AggregatorParsers;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.PipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.children.ChildrenAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.children.InternalChildren;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter;
import org.elasticsearch.search.aggregations.bucket.filters.FiltersAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filters.InternalFilters;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoGridAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoHashGridParser;
import org.elasticsearch.search.aggregations.bucket.geogrid.InternalGeoHashGrid;
import org.elasticsearch.search.aggregations.bucket.global.GlobalAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.global.InternalGlobal;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramParser;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramParser;
import org.elasticsearch.search.aggregations.bucket.histogram.InternalHistogram;
import org.elasticsearch.search.aggregations.bucket.missing.InternalMissing;
import org.elasticsearch.search.aggregations.bucket.missing.MissingAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.missing.MissingParser;
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested;
import org.elasticsearch.search.aggregations.bucket.nested.InternalReverseNested;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ReverseNestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.InternalBinaryRange;
import org.elasticsearch.search.aggregations.bucket.range.InternalRange;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.RangeParser;
import org.elasticsearch.search.aggregations.bucket.range.date.DateRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.date.DateRangeParser;
import org.elasticsearch.search.aggregations.bucket.range.date.InternalDateRange;
import org.elasticsearch.search.aggregations.bucket.range.geodistance.GeoDistanceAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.geodistance.GeoDistanceParser;
import org.elasticsearch.search.aggregations.bucket.range.geodistance.InternalGeoDistance;
import org.elasticsearch.search.aggregations.bucket.range.ip.IpRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.ip.IpRangeParser;
import org.elasticsearch.search.aggregations.bucket.sampler.DiversifiedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.sampler.DiversifiedSamplerParser;
import org.elasticsearch.search.aggregations.bucket.sampler.InternalSampler;
import org.elasticsearch.search.aggregations.bucket.sampler.SamplerAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.sampler.UnmappedSampler;
import org.elasticsearch.search.aggregations.bucket.significant.SignificantLongTerms;
import org.elasticsearch.search.aggregations.bucket.significant.SignificantStringTerms;
import org.elasticsearch.search.aggregations.bucket.significant.SignificantTermsAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.significant.SignificantTermsParser;
import org.elasticsearch.search.aggregations.bucket.significant.UnmappedSignificantTerms;
import org.elasticsearch.search.aggregations.bucket.significant.heuristics.ChiSquare;
import org.elasticsearch.search.aggregations.bucket.significant.heuristics.GND;
import org.elasticsearch.search.aggregations.bucket.significant.heuristics.JLHScore;
import org.elasticsearch.search.aggregations.bucket.significant.heuristics.MutualInformation;
import org.elasticsearch.search.aggregations.bucket.significant.heuristics.PercentageScore;
import org.elasticsearch.search.aggregations.bucket.significant.heuristics.ScriptHeuristic;
import org.elasticsearch.search.aggregations.bucket.significant.heuristics.SignificanceHeuristic;
import org.elasticsearch.search.aggregations.bucket.significant.heuristics.SignificanceHeuristicParser;
import org.elasticsearch.search.aggregations.bucket.terms.DoubleTerms;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsParser;
import org.elasticsearch.search.aggregations.bucket.terms.UnmappedTerms;
import org.elasticsearch.search.aggregations.metrics.avg.AvgAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.AvgParser;
import org.elasticsearch.search.aggregations.metrics.avg.InternalAvg;
import org.elasticsearch.search.aggregations.metrics.cardinality.CardinalityAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.cardinality.CardinalityParser;
import org.elasticsearch.search.aggregations.metrics.cardinality.InternalCardinality;
import org.elasticsearch.search.aggregations.metrics.geobounds.GeoBoundsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.geobounds.GeoBoundsParser;
import org.elasticsearch.search.aggregations.metrics.geobounds.InternalGeoBounds;
import org.elasticsearch.search.aggregations.metrics.geocentroid.GeoCentroidAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.geocentroid.GeoCentroidParser;
import org.elasticsearch.search.aggregations.metrics.geocentroid.InternalGeoCentroid;
import org.elasticsearch.search.aggregations.metrics.max.InternalMax;
import org.elasticsearch.search.aggregations.metrics.max.MaxAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.max.MaxParser;
import org.elasticsearch.search.aggregations.metrics.min.InternalMin;
import org.elasticsearch.search.aggregations.metrics.min.MinAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.min.MinParser;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentileRanksAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentileRanksParser;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentilesAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentilesParser;
import org.elasticsearch.search.aggregations.metrics.percentiles.hdr.InternalHDRPercentileRanks;
import org.elasticsearch.search.aggregations.metrics.percentiles.hdr.InternalHDRPercentiles;
import org.elasticsearch.search.aggregations.metrics.percentiles.tdigest.InternalTDigestPercentileRanks;
import org.elasticsearch.search.aggregations.metrics.percentiles.tdigest.InternalTDigestPercentiles;
import org.elasticsearch.search.aggregations.metrics.scripted.InternalScriptedMetric;
import org.elasticsearch.search.aggregations.metrics.scripted.ScriptedMetricAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.stats.InternalStats;
import org.elasticsearch.search.aggregations.metrics.stats.StatsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.stats.StatsParser;
import org.elasticsearch.search.aggregations.metrics.stats.extended.ExtendedStatsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.stats.extended.ExtendedStatsParser;
import org.elasticsearch.search.aggregations.metrics.stats.extended.InternalExtendedStats;
import org.elasticsearch.search.aggregations.metrics.sum.InternalSum;
import org.elasticsearch.search.aggregations.metrics.sum.SumAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.sum.SumParser;
import org.elasticsearch.search.aggregations.metrics.tophits.InternalTopHits;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.valuecount.InternalValueCount;
import org.elasticsearch.search.aggregations.metrics.valuecount.ValueCountAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.valuecount.ValueCountParser;
import org.elasticsearch.search.aggregations.pipeline.InternalSimpleValue;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.InternalBucketMetricValue;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.avg.AvgBucketPipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.avg.AvgBucketPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.max.MaxBucketPipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.max.MaxBucketPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.min.MinBucketPipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.min.MinBucketPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.percentile.PercentilesBucketPipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.percentile.PercentilesBucketPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.stats.InternalStatsBucket;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.stats.StatsBucketPipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.stats.StatsBucketPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.stats.extended.ExtendedStatsBucketParser;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.stats.extended.ExtendedStatsBucketPipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.stats.extended.ExtendedStatsBucketPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.stats.extended.InternalExtendedStatsBucket;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.sum.SumBucketPipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.bucketmetrics.sum.SumBucketPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketscript.BucketScriptPipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.bucketscript.BucketScriptPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.bucketselector.BucketSelectorPipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.bucketselector.BucketSelectorPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.cumulativesum.CumulativeSumPipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.cumulativesum.CumulativeSumPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.derivative.DerivativePipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.derivative.DerivativePipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.derivative.InternalDerivative;
import org.elasticsearch.search.aggregations.pipeline.movavg.MovAvgPipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.movavg.MovAvgPipelineAggregator;
import org.elasticsearch.search.aggregations.pipeline.movavg.models.EwmaModel;
import org.elasticsearch.search.aggregations.pipeline.movavg.models.HoltLinearModel;
import org.elasticsearch.search.aggregations.pipeline.movavg.models.HoltWintersModel;
import org.elasticsearch.search.aggregations.pipeline.movavg.models.LinearModel;
import org.elasticsearch.search.aggregations.pipeline.movavg.models.MovAvgModel;
import org.elasticsearch.search.aggregations.pipeline.movavg.models.SimpleModel;
import org.elasticsearch.search.aggregations.pipeline.serialdiff.SerialDiffPipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.serialdiff.SerialDiffPipelineAggregator;
import org.elasticsearch.search.controller.SearchPhaseController;
import org.elasticsearch.search.fetch.FetchPhase;
import org.elasticsearch.search.fetch.FetchSubPhase;
import org.elasticsearch.search.fetch.explain.ExplainFetchSubPhase;
import org.elasticsearch.search.fetch.fielddata.FieldDataFieldsFetchSubPhase;
import org.elasticsearch.search.fetch.matchedqueries.MatchedQueriesFetchSubPhase;
import org.elasticsearch.search.fetch.parent.ParentFieldSubFetchPhase;
import org.elasticsearch.search.fetch.script.ScriptFieldsFetchSubPhase;
import org.elasticsearch.search.fetch.source.FetchSourceSubPhase;
import org.elasticsearch.search.fetch.version.VersionFetchSubPhase;
import org.elasticsearch.search.highlight.FastVectorHighlighter;
import org.elasticsearch.search.highlight.HighlightPhase;
import org.elasticsearch.search.highlight.Highlighter;
import org.elasticsearch.search.highlight.PlainHighlighter;
import org.elasticsearch.search.highlight.PostingsHighlighter;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.elasticsearch.search.rescore.RescoreBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.suggest.Suggester;
import org.elasticsearch.search.suggest.Suggesters;
import org.elasticsearch.search.suggest.SuggestionBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggester;
import org.elasticsearch.search.suggest.phrase.Laplace;
import org.elasticsearch.search.suggest.phrase.LinearInterpolation;
import org.elasticsearch.search.suggest.phrase.PhraseSuggester;
import org.elasticsearch.search.suggest.phrase.SmoothingModel;
import org.elasticsearch.search.suggest.phrase.StupidBackoff;
import org.elasticsearch.search.suggest.term.TermSuggester;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * Sets up things that can be done at search time like queries, aggregations, and suggesters.
 */
public class SearchModule extends AbstractModule {

    private final boolean transportClient;
    private final Map<String, Highlighter> highlighters;
    private final Map<String, Suggester<?>> suggesters;
    private final ParseFieldRegistry<ScoreFunctionParser<?>> scoreFunctionParserRegistry = new ParseFieldRegistry<>("score_function");
    private final IndicesQueriesRegistry queryParserRegistry = new IndicesQueriesRegistry();
    private final ParseFieldRegistry<Aggregator.Parser> aggregationParserRegistry = new ParseFieldRegistry<>("aggregation");
    private final ParseFieldRegistry<PipelineAggregator.Parser> pipelineAggregationParserRegistry = new ParseFieldRegistry<>(
            "pipline_aggregation");
    private final AggregatorParsers aggregatorParsers = new AggregatorParsers(aggregationParserRegistry, pipelineAggregationParserRegistry);
    private final ParseFieldRegistry<SignificanceHeuristicParser> significanceHeuristicParserRegistry = new ParseFieldRegistry<>(
            "significance_heuristic");
    private final ParseFieldRegistry<MovAvgModel.AbstractModelParser> movingAverageModelParserRegistry = new ParseFieldRegistry<>(
            "moving_avg_model");

    private final List<FetchSubPhase> fetchSubPhases = new ArrayList<>();

    private final Settings settings;
    private final NamedWriteableRegistry namedWriteableRegistry;
    public static final Setting<Integer> INDICES_MAX_CLAUSE_COUNT_SETTING = Setting.intSetting("indices.query.bool.max_clause_count",
        1024, 1, Integer.MAX_VALUE, Setting.Property.NodeScope);

    // pkg private so tests can mock
    Class<? extends SearchService> searchServiceImpl = SearchService.class;

    public SearchModule(Settings settings, NamedWriteableRegistry namedWriteableRegistry, boolean transportClient,
            List<SearchPlugin> plugins) {
        this.settings = settings;
        this.namedWriteableRegistry = namedWriteableRegistry;
        this.transportClient = transportClient;
        suggesters = setupSuggesters(plugins);
        highlighters = setupHighlighters(settings, plugins);
        registerScoreFunctions(plugins);
        registerBuiltinQueryParsers();
        registerRescorers();
        registerSorts();
        registerValueFormats();
        registerSignificanceHeuristics(plugins);
        registerMovingAverageModels(plugins);
        registerBuiltinAggregations();
        registerFetchSubPhases(plugins);
    }

    /**
     * Register a query.
     *
     * @param reader the reader registered for this query's builder. Typically a reference to a constructor that takes a
     *        {@link org.elasticsearch.common.io.stream.StreamInput}
     * @param queryParser the parser the reads the query builder from xcontent
     * @param queryName holds the names by which this query might be parsed. The {@link ParseField#getPreferredName()} is special as it
     *        is the name by under which the reader is registered. So it is the name that the query should use as its
     *        {@link NamedWriteable#getWriteableName()} too.
     */
    public <QB extends QueryBuilder> void registerQuery(Writeable.Reader<QB> reader, QueryParser<QB> queryParser,
                                                                         ParseField queryName) {
        queryParserRegistry.register(queryParser, queryName);
        namedWriteableRegistry.register(QueryBuilder.class, queryName.getPreferredName(), reader);
    }

    public Suggesters getSuggesters() {
        return new Suggesters(suggesters);
    }

    public IndicesQueriesRegistry getQueryParserRegistry() {
        return queryParserRegistry;
    }

    /**
     * Returns the {@link Highlighter} registry
     */
    public Map<String, Highlighter> getHighlighters() {
        return highlighters;
    }

    /**
     * The registry of {@link SignificanceHeuristic}s.
     */
    public ParseFieldRegistry<SignificanceHeuristicParser> getSignificanceHeuristicParserRegistry() {
        return significanceHeuristicParserRegistry;
    }

    /**
     * The registry of {@link MovAvgModel}s.
     */
    public ParseFieldRegistry<MovAvgModel.AbstractModelParser> getMovingAverageMdelParserRegistry() {
        return movingAverageModelParserRegistry;
    }

    /**
     * Register an aggregation.
     */
    public void registerAggregation(AggregationSpec spec) {
        namedWriteableRegistry.register(AggregationBuilder.class, spec.aggregationName.getPreferredName(), spec.builderReader);
        aggregationParserRegistry.register(spec.aggregationParser, spec.aggregationName);
        for (Map.Entry<String, Writeable.Reader<? extends InternalAggregation>> t : spec.resultReaders.entrySet()) {
            String writeableName = t.getKey();
            Writeable.Reader<? extends InternalAggregation> internalReader = t.getValue();
            namedWriteableRegistry.register(InternalAggregation.class, writeableName, internalReader);
        }
    }

    public static class AggregationSpec {
        private final Map<String, Writeable.Reader<? extends InternalAggregation>> resultReaders = new TreeMap<>();
        private final Writeable.Reader<? extends AggregationBuilder> builderReader;
        private final Aggregator.Parser aggregationParser;
        private final ParseField aggregationName;

        /**
         * Register an aggregation.
         *
         * @param builderReader reads the {@link AggregationBuilder} from a stream
         * @param aggregationParser reads the aggregation builder from XContent
         * @param aggregationName names by which the aggregation may be parsed. The first name is special because it is the name that the
         *          reader is registered under.
         */
        public AggregationSpec(Reader<? extends AggregationBuilder> builderReader, Parser aggregationParser, ParseField aggregationName) {
            this.builderReader = builderReader;
            this.aggregationParser = aggregationParser;
            this.aggregationName = aggregationName;
        }

        /**
         * Add a reader for the shard level results of the aggregation with {@linkplain #aggregationName}'s
         * {@link ParseField#getPreferredName()} as the {@link NamedWriteable#getWriteableName()}.
         */
        public AggregationSpec addResultReader(Writeable.Reader<? extends InternalAggregation> resultReader) {
            return addResultReader(aggregationName.getPreferredName(), resultReader);
        }

        /**
         * Add a reader for the shard level results of the aggregation.
         */
        public AggregationSpec addResultReader(String writeableName, Writeable.Reader<? extends InternalAggregation> resultReader) {
            resultReaders.put(writeableName, resultReader);
            return this;
        }
    }

    /**
     * Register a pipeline aggregation.
     *
     * @param reader reads the aggregation builder from a stream
     * @param internalReader reads the {@link PipelineAggregator} from a stream
     * @param bucketReader reads the {@link InternalAggregation} that represents a bucket in this aggregation from a stream
     * @param aggregationParser reads the aggregation builder from XContent
     * @param aggregationName names by which the aggregation may be parsed. The first name is special because it is the name that the reader
     *        is registered under.
     */
    public void registerPipelineAggregation(Writeable.Reader<? extends PipelineAggregationBuilder> reader,
            Writeable.Reader<? extends PipelineAggregator> internalReader, Writeable.Reader<? extends InternalAggregation> bucketReader,
            PipelineAggregator.Parser aggregationParser, ParseField aggregationName) {
        if (false == transportClient) {
            pipelineAggregationParserRegistry.register(aggregationParser, aggregationName);
        }
        namedWriteableRegistry.register(PipelineAggregationBuilder.class, aggregationName.getPreferredName(), reader);
        namedWriteableRegistry.register(PipelineAggregator.class, aggregationName.getPreferredName(), internalReader);
        namedWriteableRegistry.register(InternalAggregation.class, aggregationName.getPreferredName(), bucketReader);
    }

    public void registerPipelineAggregation(Writeable.Reader<? extends PipelineAggregationBuilder> reader,
            PipelineAggregator.Parser aggregationParser, ParseField aggregationName) {
        // NORELEASE remove me in favor of the above method
        pipelineAggregationParserRegistry.register(aggregationParser, aggregationName);
        namedWriteableRegistry.register(PipelineAggregationBuilder.class, aggregationName.getPreferredName(), reader);
    }


    @Override
    protected void configure() {
        if (false == transportClient) {
            /*
             * Nothing is bound for transport client *but* SearchModule is still responsible for settings up the things like the
             * NamedWriteableRegistry.
             */
            bind(IndicesQueriesRegistry.class).toInstance(queryParserRegistry);
            bind(Suggesters.class).toInstance(getSuggesters());
            configureSearch();
            bind(AggregatorParsers.class).toInstance(aggregatorParsers);
        }
        configureShapes();
    }

    private void registerBuiltinAggregations() {
        registerAggregation(new AggregationSpec(AvgAggregationBuilder::new, new AvgParser(), AvgAggregationBuilder.AGGREGATION_NAME_FIELD)
                .addResultReader(InternalAvg::new));
        registerAggregation(new AggregationSpec(SumAggregationBuilder::new, new SumParser(), SumAggregationBuilder.AGGREGATION_NAME_FIELD)
                .addResultReader(InternalSum::new));
        registerAggregation(new AggregationSpec(MinAggregationBuilder::new, new MinParser(), MinAggregationBuilder.AGGREGATION_NAME_FIELD)
                .addResultReader(InternalMin::new));
        registerAggregation(new AggregationSpec(MaxAggregationBuilder::new, new MaxParser(), MaxAggregationBuilder.AGGREGATION_NAME_FIELD)
                .addResultReader(InternalMax::new));
        registerAggregation(new AggregationSpec(StatsAggregationBuilder::new, new StatsParser(),
                StatsAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalStats::new));
        registerAggregation(new AggregationSpec(ExtendedStatsAggregationBuilder::new, new ExtendedStatsParser(),
                ExtendedStatsAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalExtendedStats::new));
        registerAggregation(new AggregationSpec(ValueCountAggregationBuilder::new, new ValueCountParser(),
                ValueCountAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalValueCount::new));
        registerAggregation(new AggregationSpec(PercentilesAggregationBuilder::new, new PercentilesParser(),
                PercentilesAggregationBuilder.AGGREGATION_NAME_FIELD)
                    .addResultReader(InternalTDigestPercentiles.NAME, InternalTDigestPercentiles::new)
                    .addResultReader(InternalHDRPercentiles.NAME, InternalHDRPercentiles::new));
        registerAggregation(new AggregationSpec(PercentileRanksAggregationBuilder::new, new PercentileRanksParser(),
                PercentileRanksAggregationBuilder.AGGREGATION_NAME_FIELD)
                    .addResultReader(InternalTDigestPercentileRanks.NAME, InternalTDigestPercentileRanks::new)
                    .addResultReader(InternalHDRPercentileRanks.NAME, InternalHDRPercentileRanks::new));
        registerAggregation(new AggregationSpec(CardinalityAggregationBuilder::new, new CardinalityParser(),
                CardinalityAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalCardinality::new));
        registerAggregation(new AggregationSpec(GlobalAggregationBuilder::new, GlobalAggregationBuilder::parse,
                GlobalAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalGlobal::new));
        registerAggregation(
                new AggregationSpec(MissingAggregationBuilder::new, new MissingParser(), MissingAggregationBuilder.AGGREGATION_NAME_FIELD)
                        .addResultReader(InternalMissing::new));
        registerAggregation(new AggregationSpec(FilterAggregationBuilder::new, FilterAggregationBuilder::parse,
                FilterAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalFilter::new));
        registerAggregation(new AggregationSpec(FiltersAggregationBuilder::new, FiltersAggregationBuilder::parse,
                FiltersAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalFilters::new));
        registerAggregation(new AggregationSpec(SamplerAggregationBuilder::new, SamplerAggregationBuilder::parse,
                SamplerAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalSampler.NAME, InternalSampler::new)
                        .addResultReader(UnmappedSampler.NAME, UnmappedSampler::new));
        registerAggregation(new AggregationSpec(DiversifiedAggregationBuilder::new, new DiversifiedSamplerParser(),
                DiversifiedAggregationBuilder.AGGREGATION_NAME_FIELD));
        registerAggregation(
                new AggregationSpec(TermsAggregationBuilder::new, new TermsParser(), TermsAggregationBuilder.AGGREGATION_NAME_FIELD)
                    .addResultReader(StringTerms.NAME, StringTerms::new)
                    .addResultReader(UnmappedTerms.NAME, UnmappedTerms::new)
                    .addResultReader(LongTerms.NAME, LongTerms::new)
                    .addResultReader(DoubleTerms.NAME, DoubleTerms::new));
        registerAggregation(new AggregationSpec(SignificantTermsAggregationBuilder::new,
                new SignificantTermsParser(significanceHeuristicParserRegistry, queryParserRegistry),
                SignificantTermsAggregationBuilder.AGGREGATION_NAME_FIELD)
                    .addResultReader(SignificantStringTerms.NAME, SignificantStringTerms::new)
                    .addResultReader(SignificantLongTerms.NAME, SignificantLongTerms::new)
                    .addResultReader(UnmappedSignificantTerms.NAME, UnmappedSignificantTerms::new));
        registerAggregation(new AggregationSpec(RangeAggregationBuilder::new, new RangeParser(),
                RangeAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalRange::new));
        registerAggregation(new AggregationSpec(DateRangeAggregationBuilder::new, new DateRangeParser(),
                DateRangeAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalDateRange::new));
        registerAggregation(
                new AggregationSpec(IpRangeAggregationBuilder::new, new IpRangeParser(), IpRangeAggregationBuilder.AGGREGATION_NAME_FIELD)
                        .addResultReader(InternalBinaryRange::new));
        registerAggregation(new AggregationSpec(HistogramAggregationBuilder::new, new HistogramParser(),
                HistogramAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalHistogram::new));
        registerAggregation(new AggregationSpec(DateHistogramAggregationBuilder::new, new DateHistogramParser(),
                DateHistogramAggregationBuilder.AGGREGATION_NAME_FIELD));
        registerAggregation(new AggregationSpec(GeoDistanceAggregationBuilder::new, new GeoDistanceParser(),
                GeoDistanceAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalGeoDistance::new));
        registerAggregation(new AggregationSpec(GeoGridAggregationBuilder::new, new GeoHashGridParser(),
                GeoGridAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalGeoHashGrid::new));
        registerAggregation(new AggregationSpec(NestedAggregationBuilder::new, NestedAggregationBuilder::parse,
                NestedAggregationBuilder.AGGREGATION_FIELD_NAME).addResultReader(InternalNested::new));
        registerAggregation(new AggregationSpec(ReverseNestedAggregationBuilder::new, ReverseNestedAggregationBuilder::parse,
                ReverseNestedAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalReverseNested::new));
        registerAggregation(new AggregationSpec(TopHitsAggregationBuilder::new, TopHitsAggregationBuilder::parse,
                TopHitsAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalTopHits::new));
        registerAggregation(new AggregationSpec(GeoBoundsAggregationBuilder::new, new GeoBoundsParser(),
                GeoBoundsAggregationBuilder.AGGREGATION_NAME_FIED).addResultReader(InternalGeoBounds::new));
        registerAggregation(new AggregationSpec(GeoCentroidAggregationBuilder::new, new GeoCentroidParser(),
                GeoCentroidAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalGeoCentroid::new));
        registerAggregation(new AggregationSpec(ScriptedMetricAggregationBuilder::new, ScriptedMetricAggregationBuilder::parse,
                ScriptedMetricAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalScriptedMetric::new));
        registerAggregation(new AggregationSpec(ChildrenAggregationBuilder::new, ChildrenAggregationBuilder::parse,
                ChildrenAggregationBuilder.AGGREGATION_NAME_FIELD).addResultReader(InternalChildren::new));

        registerPipelineAggregation(DerivativePipelineAggregationBuilder::new, DerivativePipelineAggregator::new, InternalDerivative::new,
                DerivativePipelineAggregationBuilder::parse, DerivativePipelineAggregationBuilder.AGGREGATION_NAME_FIELD);
        registerPipelineAggregation(MaxBucketPipelineAggregationBuilder::new, MaxBucketPipelineAggregationBuilder.PARSER,
                MaxBucketPipelineAggregationBuilder.AGGREGATION_NAME_FIELD);
        registerPipelineAggregation(MinBucketPipelineAggregationBuilder::new, MinBucketPipelineAggregationBuilder.PARSER,
                MinBucketPipelineAggregationBuilder.AGGREGATION_FIELD_NAME);
        registerPipelineAggregation(AvgBucketPipelineAggregationBuilder::new, AvgBucketPipelineAggregationBuilder.PARSER,
                AvgBucketPipelineAggregationBuilder.AGGREGATION_NAME_FIELD);
        registerPipelineAggregation(SumBucketPipelineAggregationBuilder::new, SumBucketPipelineAggregationBuilder.PARSER,
                SumBucketPipelineAggregationBuilder.AGGREGATION_NAME_FIELD);
        registerPipelineAggregation(StatsBucketPipelineAggregationBuilder::new, StatsBucketPipelineAggregator::new,
                InternalStatsBucket::new, StatsBucketPipelineAggregationBuilder.PARSER,
                StatsBucketPipelineAggregationBuilder.AGGREGATION_NAME_FIELD);
        registerPipelineAggregation(ExtendedStatsBucketPipelineAggregationBuilder::new, ExtendedStatsBucketPipelineAggregator::new,
                InternalExtendedStatsBucket::new, new ExtendedStatsBucketParser(),
                ExtendedStatsBucketPipelineAggregationBuilder.AGGREGATION_NAME_FIELD);
        registerPipelineAggregation(PercentilesBucketPipelineAggregationBuilder::new, PercentilesBucketPipelineAggregationBuilder.PARSER,
                PercentilesBucketPipelineAggregationBuilder.AGGREGATION_NAME_FIELD);
        registerPipelineAggregation(MovAvgPipelineAggregationBuilder::new,
                (n, c) -> MovAvgPipelineAggregationBuilder.parse(movingAverageModelParserRegistry, n, c),
                MovAvgPipelineAggregationBuilder.AGGREGATION_FIELD_NAME);
        registerPipelineAggregation(CumulativeSumPipelineAggregationBuilder::new, CumulativeSumPipelineAggregationBuilder::parse,
                CumulativeSumPipelineAggregationBuilder.AGGREGATION_NAME_FIELD);
        registerPipelineAggregation(BucketScriptPipelineAggregationBuilder::new, BucketScriptPipelineAggregationBuilder::parse,
                BucketScriptPipelineAggregationBuilder.AGGREGATION_NAME_FIELD);
        registerPipelineAggregation(BucketSelectorPipelineAggregationBuilder::new, BucketSelectorPipelineAggregationBuilder::parse,
                BucketSelectorPipelineAggregationBuilder.AGGREGATION_NAME_FIELD);
        registerPipelineAggregation(SerialDiffPipelineAggregationBuilder::new, SerialDiffPipelineAggregationBuilder::parse,
                SerialDiffPipelineAggregationBuilder.AGGREGATION_NAME_FIELD);
    }

    protected void configureSearch() {
        // configure search private classes...
        bind(SearchPhaseController.class).asEagerSingleton();
        bind(FetchPhase.class).toInstance(new FetchPhase(fetchSubPhases));
        bind(SearchTransportService.class).asEagerSingleton();
        if (searchServiceImpl == SearchService.class) {
            bind(SearchService.class).asEagerSingleton();
        } else {
            bind(SearchService.class).to(searchServiceImpl).asEagerSingleton();
        }
    }

    private void configureShapes() {
        if (ShapesAvailability.JTS_AVAILABLE && ShapesAvailability.SPATIAL4J_AVAILABLE) {
            ShapeBuilders.register(namedWriteableRegistry);
        }
    }

    private void registerRescorers() {
        namedWriteableRegistry.register(RescoreBuilder.class, QueryRescorerBuilder.NAME, QueryRescorerBuilder::new);
    }

    private void registerSorts() {
        namedWriteableRegistry.register(SortBuilder.class, GeoDistanceSortBuilder.NAME, GeoDistanceSortBuilder::new);
        namedWriteableRegistry.register(SortBuilder.class, ScoreSortBuilder.NAME, ScoreSortBuilder::new);
        namedWriteableRegistry.register(SortBuilder.class, ScriptSortBuilder.NAME, ScriptSortBuilder::new);
        namedWriteableRegistry.register(SortBuilder.class, FieldSortBuilder.NAME, FieldSortBuilder::new);
    }

    private <T> void registerFromPlugin(List<SearchPlugin> plugins, Function<SearchPlugin, List<T>> producer, Consumer<T> consumer) {
        for (SearchPlugin plugin : plugins) {
            for (T t : producer.apply(plugin)) {
                consumer.accept(t);
            }
        }
    }

    public static void registerSmoothingModels(NamedWriteableRegistry namedWriteableRegistry) {
        namedWriteableRegistry.register(SmoothingModel.class, Laplace.NAME, Laplace::new);
        namedWriteableRegistry.register(SmoothingModel.class, LinearInterpolation.NAME, LinearInterpolation::new);
        namedWriteableRegistry.register(SmoothingModel.class, StupidBackoff.NAME, StupidBackoff::new);
    }

    private Map<String, Suggester<?>> setupSuggesters(List<SearchPlugin> plugins) {
        registerSmoothingModels(namedWriteableRegistry);

        // Suggester<?> is weird - it is both a Parser and a reader....
        NamedRegistry<Suggester<?>> suggesters = new NamedRegistry<Suggester<?>>("suggester") {
            @Override
            public void register(String name, Suggester<?> t) {
                super.register(name, t);
                namedWriteableRegistry.register(SuggestionBuilder.class, name, t);
            }
        };
        suggesters.register("phrase", PhraseSuggester.INSTANCE);
        suggesters.register("term", TermSuggester.INSTANCE);
        suggesters.register("completion", CompletionSuggester.INSTANCE);

        suggesters.extractAndRegister(plugins, SearchPlugin::getSuggesters);
        return unmodifiableMap(suggesters.getRegistry());
    }

    private Map<String, Highlighter> setupHighlighters(Settings settings, List<SearchPlugin> plugins) {
        NamedRegistry<Highlighter> highlighters = new NamedRegistry<>("highlighter");
        highlighters.register("fvh",  new FastVectorHighlighter(settings));
        highlighters.register("plain", new PlainHighlighter());
        highlighters.register("postings", new PostingsHighlighter());

        highlighters.extractAndRegister(plugins, SearchPlugin::getHighlighters);

        return unmodifiableMap(highlighters.getRegistry());
    }

    private void registerScoreFunctions(List<SearchPlugin> plugins) {
        registerScoreFunction(new ScoreFunctionSpec<>(ScriptScoreFunctionBuilder.NAME, ScriptScoreFunctionBuilder::new,
                ScriptScoreFunctionBuilder::fromXContent));
        registerScoreFunction(
                new ScoreFunctionSpec<>(GaussDecayFunctionBuilder.NAME, GaussDecayFunctionBuilder::new, GaussDecayFunctionBuilder.PARSER));
        registerScoreFunction(new ScoreFunctionSpec<>(LinearDecayFunctionBuilder.NAME, LinearDecayFunctionBuilder::new,
                LinearDecayFunctionBuilder.PARSER));
        registerScoreFunction(new ScoreFunctionSpec<>(ExponentialDecayFunctionBuilder.NAME, ExponentialDecayFunctionBuilder::new,
                ExponentialDecayFunctionBuilder.PARSER));
        registerScoreFunction(new ScoreFunctionSpec<>(RandomScoreFunctionBuilder.NAME, RandomScoreFunctionBuilder::new,
                RandomScoreFunctionBuilder::fromXContent));
        registerScoreFunction(new ScoreFunctionSpec<>(FieldValueFactorFunctionBuilder.NAME, FieldValueFactorFunctionBuilder::new,
                FieldValueFactorFunctionBuilder::fromXContent));

        //weight doesn't have its own parser, so every function supports it out of the box.
        //Can be a single function too when not associated to any other function, which is why it needs to be registered manually here.
        namedWriteableRegistry.register(ScoreFunctionBuilder.class, WeightBuilder.NAME, WeightBuilder::new);

        registerFromPlugin(plugins, SearchPlugin::getScoreFunctions, this::registerScoreFunction);
    }

    private void registerScoreFunction(ScoreFunctionSpec<?> scoreFunction) {
        scoreFunctionParserRegistry.register(scoreFunction.getParser(), scoreFunction.getName());
        namedWriteableRegistry.register(ScoreFunctionBuilder.class, scoreFunction.getName().getPreferredName(), scoreFunction.getReader());
    }

    private void registerValueFormats() {
        registerValueFormat(DocValueFormat.BOOLEAN.getWriteableName(), in -> DocValueFormat.BOOLEAN);
        registerValueFormat(DocValueFormat.DateTime.NAME, DocValueFormat.DateTime::new);
        registerValueFormat(DocValueFormat.Decimal.NAME, DocValueFormat.Decimal::new);
        registerValueFormat(DocValueFormat.GEOHASH.getWriteableName(), in -> DocValueFormat.GEOHASH);
        registerValueFormat(DocValueFormat.IP.getWriteableName(), in -> DocValueFormat.IP);
        registerValueFormat(DocValueFormat.RAW.getWriteableName(), in -> DocValueFormat.RAW);
    }

    /**
     * Register a new ValueFormat.
     */
    private void registerValueFormat(String name, Writeable.Reader<? extends DocValueFormat> reader) {
        namedWriteableRegistry.register(DocValueFormat.class, name, reader);
    }

    private void registerSignificanceHeuristics(List<SearchPlugin> plugins) {
        registerSignificanceHeuristic(new SearchPluginSpec<>(ChiSquare.NAME, ChiSquare::new, ChiSquare.PARSER));
        registerSignificanceHeuristic(new SearchPluginSpec<>(GND.NAME, GND::new, GND.PARSER));
        registerSignificanceHeuristic(new SearchPluginSpec<>(JLHScore.NAME, JLHScore::new, JLHScore::parse));
        registerSignificanceHeuristic(new SearchPluginSpec<>(MutualInformation.NAME, MutualInformation::new, MutualInformation.PARSER));
        registerSignificanceHeuristic(new SearchPluginSpec<>(PercentageScore.NAME, PercentageScore::new, PercentageScore::parse));
        registerSignificanceHeuristic(new SearchPluginSpec<>(ScriptHeuristic.NAME, ScriptHeuristic::new, ScriptHeuristic::parse));

        registerFromPlugin(plugins, SearchPlugin::getSignificanceHeuristics, this::registerSignificanceHeuristic);
    }

    private void registerSignificanceHeuristic(SearchPluginSpec<SignificanceHeuristic, SignificanceHeuristicParser> heuristic) {
        significanceHeuristicParserRegistry.register(heuristic.getParser(), heuristic.getName());
        namedWriteableRegistry.register(SignificanceHeuristic.class, heuristic.getName().getPreferredName(), heuristic.getReader());
    }

    private void registerMovingAverageModels(List<SearchPlugin> plugins) {
        registerMovingAverageModel(new SearchPluginSpec<>(SimpleModel.NAME, SimpleModel::new, SimpleModel.PARSER));
        registerMovingAverageModel(new SearchPluginSpec<>(LinearModel.NAME, LinearModel::new, LinearModel.PARSER));
        registerMovingAverageModel(new SearchPluginSpec<>(EwmaModel.NAME, EwmaModel::new, EwmaModel.PARSER));
        registerMovingAverageModel(new SearchPluginSpec<>(HoltLinearModel.NAME, HoltLinearModel::new, HoltLinearModel.PARSER));
        registerMovingAverageModel(new SearchPluginSpec<>(HoltWintersModel.NAME, HoltWintersModel::new, HoltWintersModel.PARSER));

        registerFromPlugin(plugins, SearchPlugin::getMovingAverageModels, this::registerMovingAverageModel);
    }

    private void registerMovingAverageModel(SearchPluginSpec<MovAvgModel, MovAvgModel.AbstractModelParser> movAvgModel) {
        movingAverageModelParserRegistry.register(movAvgModel.getParser(), movAvgModel.getName());
        namedWriteableRegistry.register(MovAvgModel.class, movAvgModel.getName().getPreferredName(), movAvgModel.getReader());
    }

    private void registerFetchSubPhases(List<SearchPlugin> plugins) {
        registerFetchSubPhase(new ExplainFetchSubPhase());
        registerFetchSubPhase(new FieldDataFieldsFetchSubPhase());
        registerFetchSubPhase(new ScriptFieldsFetchSubPhase());
        registerFetchSubPhase(new FetchSourceSubPhase());
        registerFetchSubPhase(new VersionFetchSubPhase());
        registerFetchSubPhase(new MatchedQueriesFetchSubPhase());
        registerFetchSubPhase(new HighlightPhase(settings, highlighters));
        registerFetchSubPhase(new ParentFieldSubFetchPhase());

        FetchPhaseConstructionContext context = new FetchPhaseConstructionContext(highlighters);
        registerFromPlugin(plugins, p -> p.getFetchSubPhases(context), this::registerFetchSubPhase);
    }

    private void registerFetchSubPhase(FetchSubPhase subPhase) {
        Class<?> subPhaseClass = subPhase.getClass();
        if (fetchSubPhases.stream().anyMatch(p -> p.getClass().equals(subPhaseClass))) {
            throw new IllegalArgumentException("FetchSubPhase [" + subPhaseClass + "] already registered");
        }
        fetchSubPhases.add(requireNonNull(subPhase, "FetchSubPhase must not be null"));
    }

    private void registerBuiltinQueryParsers() {
        registerQuery(MatchQueryBuilder::new, MatchQueryBuilder::fromXContent, MatchQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(MatchPhraseQueryBuilder::new, MatchPhraseQueryBuilder::fromXContent, MatchPhraseQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(MatchPhrasePrefixQueryBuilder::new, MatchPhrasePrefixQueryBuilder::fromXContent,
                MatchPhrasePrefixQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(MultiMatchQueryBuilder::new, MultiMatchQueryBuilder::fromXContent, MultiMatchQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(NestedQueryBuilder::new, NestedQueryBuilder::fromXContent, NestedQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(HasChildQueryBuilder::new, HasChildQueryBuilder::fromXContent, HasChildQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(HasParentQueryBuilder::new, HasParentQueryBuilder::fromXContent, HasParentQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(DisMaxQueryBuilder::new, DisMaxQueryBuilder::fromXContent, DisMaxQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(IdsQueryBuilder::new, IdsQueryBuilder::fromXContent, IdsQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(MatchAllQueryBuilder::new, MatchAllQueryBuilder::fromXContent, MatchAllQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(QueryStringQueryBuilder::new, QueryStringQueryBuilder::fromXContent, QueryStringQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(BoostingQueryBuilder::new, BoostingQueryBuilder::fromXContent, BoostingQueryBuilder.QUERY_NAME_FIELD);
        BooleanQuery.setMaxClauseCount(INDICES_MAX_CLAUSE_COUNT_SETTING.get(settings));
        registerQuery(BoolQueryBuilder::new, BoolQueryBuilder::fromXContent, BoolQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(TermQueryBuilder::new, TermQueryBuilder::fromXContent, TermQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(TermsQueryBuilder::new, TermsQueryBuilder::fromXContent, TermsQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(FuzzyQueryBuilder::new, FuzzyQueryBuilder::fromXContent, FuzzyQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(RegexpQueryBuilder::new, RegexpQueryBuilder::fromXContent, RegexpQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(RangeQueryBuilder::new, RangeQueryBuilder::fromXContent, RangeQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(PrefixQueryBuilder::new, PrefixQueryBuilder::fromXContent, PrefixQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(WildcardQueryBuilder::new, WildcardQueryBuilder::fromXContent, WildcardQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(ConstantScoreQueryBuilder::new, ConstantScoreQueryBuilder::fromXContent, ConstantScoreQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(SpanTermQueryBuilder::new, SpanTermQueryBuilder::fromXContent, SpanTermQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(SpanNotQueryBuilder::new, SpanNotQueryBuilder::fromXContent, SpanNotQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(SpanWithinQueryBuilder::new, SpanWithinQueryBuilder::fromXContent, SpanWithinQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(SpanContainingQueryBuilder::new, SpanContainingQueryBuilder::fromXContent,
                SpanContainingQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(FieldMaskingSpanQueryBuilder::new, FieldMaskingSpanQueryBuilder::fromXContent,
                FieldMaskingSpanQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(SpanFirstQueryBuilder::new, SpanFirstQueryBuilder::fromXContent, SpanFirstQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(SpanNearQueryBuilder::new, SpanNearQueryBuilder::fromXContent, SpanNearQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(SpanOrQueryBuilder::new, SpanOrQueryBuilder::fromXContent, SpanOrQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(MoreLikeThisQueryBuilder::new, MoreLikeThisQueryBuilder::fromXContent, MoreLikeThisQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(WrapperQueryBuilder::new, WrapperQueryBuilder::fromXContent, WrapperQueryBuilder.QUERY_NAME_FIELD);
        // TODO Remove IndicesQuery in 6.0
        registerQuery(IndicesQueryBuilder::new, IndicesQueryBuilder::fromXContent, IndicesQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(CommonTermsQueryBuilder::new, CommonTermsQueryBuilder::fromXContent, CommonTermsQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(SpanMultiTermQueryBuilder::new, SpanMultiTermQueryBuilder::fromXContent, SpanMultiTermQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(FunctionScoreQueryBuilder::new, c -> FunctionScoreQueryBuilder.fromXContent(scoreFunctionParserRegistry, c),
                FunctionScoreQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(SimpleQueryStringBuilder::new, SimpleQueryStringBuilder::fromXContent, SimpleQueryStringBuilder.QUERY_NAME_FIELD);
        registerQuery(TemplateQueryBuilder::new, TemplateQueryBuilder::fromXContent, TemplateQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(TypeQueryBuilder::new, TypeQueryBuilder::fromXContent, TypeQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(ScriptQueryBuilder::new, ScriptQueryBuilder::fromXContent, ScriptQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(GeoDistanceQueryBuilder::new, GeoDistanceQueryBuilder::fromXContent, GeoDistanceQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(GeoDistanceRangeQueryBuilder::new, GeoDistanceRangeQueryBuilder::fromXContent,
                GeoDistanceRangeQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(GeoBoundingBoxQueryBuilder::new, GeoBoundingBoxQueryBuilder::fromXContent,
                GeoBoundingBoxQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(GeohashCellQuery.Builder::new, GeohashCellQuery.Builder::fromXContent, GeohashCellQuery.QUERY_NAME_FIELD);
        registerQuery(GeoPolygonQueryBuilder::new, GeoPolygonQueryBuilder::fromXContent, GeoPolygonQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(ExistsQueryBuilder::new, ExistsQueryBuilder::fromXContent, ExistsQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(MatchNoneQueryBuilder::new, MatchNoneQueryBuilder::fromXContent, MatchNoneQueryBuilder.QUERY_NAME_FIELD);
        registerQuery(ParentIdQueryBuilder::new, ParentIdQueryBuilder::fromXContent, ParentIdQueryBuilder.QUERY_NAME_FIELD);

        if (ShapesAvailability.JTS_AVAILABLE && ShapesAvailability.SPATIAL4J_AVAILABLE) {
            registerQuery(GeoShapeQueryBuilder::new, GeoShapeQueryBuilder::fromXContent, GeoShapeQueryBuilder.QUERY_NAME_FIELD);
        }
    }

    static {
        // Pipeline Aggregations
        InternalSimpleValue.registerStreams();
        InternalBucketMetricValue.registerStreams();
        MaxBucketPipelineAggregator.registerStreams();
        MinBucketPipelineAggregator.registerStreams();
        AvgBucketPipelineAggregator.registerStreams();
        SumBucketPipelineAggregator.registerStreams();
        PercentilesBucketPipelineAggregator.registerStreams();
        MovAvgPipelineAggregator.registerStreams();
        CumulativeSumPipelineAggregator.registerStreams();
        BucketScriptPipelineAggregator.registerStreams();
        BucketSelectorPipelineAggregator.registerStreams();
        SerialDiffPipelineAggregator.registerStreams();
    }
}
