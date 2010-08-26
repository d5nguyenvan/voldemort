/*
 * Copyright 2010 LinkedIn, Inc
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

package voldemort.store.routed.action;

import voldemort.cluster.Cluster;
import voldemort.cluster.Node;
import voldemort.cluster.failuredetector.FailureDetector;
import voldemort.store.InsufficientOperationalNodesException;
import voldemort.store.Store;
import voldemort.store.UnreachableStoreException;
import voldemort.store.routed.BasicPipelineData;
import voldemort.store.routed.Pipeline;
import voldemort.store.slop.HintedHandoffStrategy;
import voldemort.store.slop.Slop;
import voldemort.utils.ByteArray;
import voldemort.versioning.Version;

import java.util.Date;
import java.util.Map;

public class PerformDeleteHintedHandoff
               extends AbstractHintedHandoffAction<Boolean, BasicPipelineData<Boolean>> {

    private final Version version;

    public PerformDeleteHintedHandoff(BasicPipelineData<Boolean> pipelineData,
                                      Pipeline.Event completeEvent,
                                      ByteArray key,
                                      Version version,
                                      FailureDetector failureDetector,
                                      Map<Integer, Store<ByteArray, Slop>> slopStores,
                                      HintedHandoffStrategy handoffStrategy) {
        super(pipelineData, completeEvent, key, failureDetector, slopStores, handoffStrategy);
        this.version = version;
    }
    
    @Override
    public void execute(Pipeline pipeline) {
        for(Node failedNode: failedNodes) {
            int failedNodeId = failedNode.getId();
            if(logger.isTraceEnabled())
                logger.trace("Performing hinted handoff for node " + failedNode + ", store "
                             + pipelineData.getStoreName() +  "key " + key + ", version"
                             + version);

            Slop slop = new Slop(pipelineData.getStoreName(),
                                 Slop.Operation.DELETE,
                                 key,
                                 null,
                                 failedNodeId,
                                 new Date());

            boolean persisted = hintedHandoff.sendHint(failedNode, version, slop);

            Exception e = pipelineData.getFatalError();
            if(e != null) {
                if(persisted)
                 pipelineData.setFatalError(new UnreachableStoreException("Delete operation failed on node "
                                                                             + failedNodeId
                                                                             + ", but has been persisted to slop storage for eventual replication.",
                                                                             e));
                else
                    pipelineData.setFatalError(new InsufficientOperationalNodesException("All slop servers are unavailable from node "
                                                                                         + failedNodeId + ".",
                                                                                         e));
            }
        }

        pipeline.addEvent(completeEvent);
    }
}