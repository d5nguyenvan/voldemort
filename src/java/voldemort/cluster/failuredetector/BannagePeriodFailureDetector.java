/*
 * Copyright 2009 Mustard Grain, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package voldemort.cluster.failuredetector;

import org.apache.log4j.Level;

import voldemort.annotations.jmx.JmxManaged;
import voldemort.client.ClientConfig;
import voldemort.cluster.Node;
import voldemort.server.VoldemortConfig;
import voldemort.store.UnreachableStoreException;

/**
 * BannagePeriodFailureDetector relies on external callers to notify it of
 * failed attempts to access a node's store via recordException. When
 * recordException is invoked, the node is marked offline for a period of time
 * as defined by the client or server configuration. Once that period has
 * passed, the node is considered <em>available</em>. However,
 * BannagePeriodFailureDetector's definition of available uses a fairly loose
 * sense of the word. Rather than considering the node to be available for
 * access, it is available for <i>attempting</i> to access. In actuality the
 * node may still be down. However, the intent is simply to mark it down for N
 * seconds and then attempt to try again and repeat. If the node is truly
 * available for access, the caller will then invoke recordSuccess and the node
 * will be marked available in the truest sense of the word.
 * 
 * @author jay
 * @author Kirk True
 * 
 * @see VoldemortConfig#getClientNodeBannageMs
 * @see ClientConfig#getNodeBannagePeriod
 * @see FailureDetectorConfig#getNodeBannagePeriod
 */

@JmxManaged(description = "Detects the availability of the nodes on which a Voldemort cluster runs")
public class BannagePeriodFailureDetector extends AbstractFailureDetector {

    public BannagePeriodFailureDetector(FailureDetectorConfig failureDetectorConfig) {
        super(failureDetectorConfig);
    }

    public void recordException(Node node, UnreachableStoreException e) {
        setUnavailable(node);

        if(logger.isEnabledFor(Level.WARN))
            logger.warn("Could not connect to node " + node.getId() + " at " + node.getHost()
                        + " marking as unavailable for " + getConfig().getNodeBannagePeriod()
                        + " ms.", e);

        if(logger.isDebugEnabled())
            logger.debug(e);
    }

    public void recordSuccess(Node node) {
        setAvailable(node);
    }

    public void destroy() {}

}