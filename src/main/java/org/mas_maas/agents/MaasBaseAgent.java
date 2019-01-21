package org.mas_maas.agents;

import java.util.concurrent.atomic.AtomicInteger;

import org.maas.agents.BaseAgent;

@SuppressWarnings("serial")
public class MaasBaseAgent extends BaseAgent {
    private AtomicInteger numBusyBehaviours = new AtomicInteger(0);
}
