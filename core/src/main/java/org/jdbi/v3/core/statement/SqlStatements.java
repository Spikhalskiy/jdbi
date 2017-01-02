/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.core.statement;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.rewriter.ColonPrefixStatementRewriter;
import org.jdbi.v3.core.rewriter.StatementRewriter;

public final class SqlStatements implements JdbiConfig<SqlStatements> {

    private final Map<String, Object> attributes;
    private StatementRewriter statementRewriter;
    private TimingCollector timingCollector;

    private boolean returningGeneratedKeys;
    private String[] generatedKeysColumnNames;
    private boolean concurrentUpdatable;

    public SqlStatements() {
        attributes = new ConcurrentHashMap<>();
        statementRewriter = new ColonPrefixStatementRewriter();
        timingCollector = TimingCollector.NOP_TIMING_COLLECTOR;
    }

    private SqlStatements(SqlStatements that) {
        this.attributes = new ConcurrentHashMap<>(that.attributes);
        this.statementRewriter = that.statementRewriter;
        this.timingCollector = that.timingCollector;
        this.returningGeneratedKeys = that.returningGeneratedKeys;
        this.generatedKeysColumnNames = that.generatedKeysColumnNames == null ? null : that.generatedKeysColumnNames.clone();
        this.concurrentUpdatable = that.concurrentUpdatable;
    }

    /**
     * Define an attribute for {@link StatementContext} for statements executed by JDBI.
     *
     * @param key   the key for the attribute
     * @param value the value for the attribute
     * @return this
     */
    public SqlStatements define(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    /**
     * Defines attributes for each key/value pair in the Map.
     *
     * @param values map of attributes to define.
     * @return this
     */
    public SqlStatements defineMap(final Map<String, ?> values) {
        if (values != null) {
            attributes.putAll(values);
        }
        return this;
    }

    /**
     * Obtain the value of an attribute
     *
     * @param key the name of the attribute
     * @return the value of the attribute
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Returns the attributes which will be applied to {@link SqlStatement SQL statements} created by JDBI.
     *
     * @return the defined attributes.
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public StatementRewriter getStatementRewriter() {
        return statementRewriter;
    }

    /**
     * Sets the {@link StatementRewriter} used to transform SQL for all {@link SqlStatement SQL satements} executed by
     * JDBI. The default statement rewriter handles named parameter interpolation.
     *
     * @param rewriter the new statement rewriter.
     * @return this
     */
    public SqlStatements setStatementRewriter(StatementRewriter rewriter) {
        this.statementRewriter = rewriter;
        return this;
    }

    public TimingCollector getTimingCollector() {
        return timingCollector;
    }

    /**
     * Sets the {@link TimingCollector} used to collect timing about the {@link SqlStatement SQL statements} executed
     * by JDBI. The default collector does nothing.
     *
     * @param timingCollector the new timing collector
     * @return this
     */
    public SqlStatements setTimingCollector(TimingCollector timingCollector) {
        this.timingCollector = timingCollector == null ? TimingCollector.NOP_TIMING_COLLECTOR : timingCollector;
        return this;
    }

    /**
     * Sets whether the current statement returns generated keys.
     * @param b return generated keys?
     */
    public void setReturningGeneratedKeys(boolean b)
    {
        if (isConcurrentUpdatable() && b) {
            throw new IllegalArgumentException("Cannot create a result set that is concurrent "
                    + "updatable and is returning generated keys.");
        }
        this.returningGeneratedKeys = b;
    }

    /**
     * @return whether the statement being generated is expected to return generated keys.
     */
    public boolean isReturningGeneratedKeys()
    {
        return returningGeneratedKeys || generatedKeysColumnNames != null && generatedKeysColumnNames.length > 0;
    }

    /**
     * @return the generated key column names, if any
     */
    public String[] getGeneratedKeysColumnNames()
    {
        if (generatedKeysColumnNames == null) {
            return new String[0];
        }
        return Arrays.copyOf(generatedKeysColumnNames, generatedKeysColumnNames.length);
    }

    /**
     * Set the generated key column names.
     * @param generatedKeysColumnNames the generated key column names
     */
    public void setGeneratedKeysColumnNames(String[] generatedKeysColumnNames)
    {
        this.generatedKeysColumnNames = Arrays.copyOf(generatedKeysColumnNames, generatedKeysColumnNames.length);
    }

    /**
     * Return if the statement should be concurrent updatable.
     *
     * If this returns true, the concurrency level of the created ResultSet will be
     * {@link java.sql.ResultSet#CONCUR_UPDATABLE}, otherwise the result set is not updatable,
     * and will have concurrency level {@link java.sql.ResultSet#CONCUR_READ_ONLY}.
     *
     * @return if the statement generated should be concurrent updatable.
     */
    public boolean isConcurrentUpdatable() {
        return concurrentUpdatable;
    }

    /**
     * Set the context to create a concurrent updatable result set.
     *
     * This cannot be combined with {@link #isReturningGeneratedKeys()}, only
     * one option may be selected. It does not make sense to combine these either, as one
     * applies to queries, and the other applies to updates.
     *
     * @param concurrentUpdatable if the result set should be concurrent updatable.
     */
    public void setConcurrentUpdatable(final boolean concurrentUpdatable) {
        if (concurrentUpdatable && isReturningGeneratedKeys()) {
            throw new IllegalArgumentException("Cannot create a result set that is concurrent "
                    + "updatable and is returning generated keys.");
        }
        this.concurrentUpdatable = concurrentUpdatable;
    }

    @Override
    public SqlStatements createCopy() {
        return new SqlStatements(this);
    }
}
