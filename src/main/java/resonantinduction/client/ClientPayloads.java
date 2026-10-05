package resonantinduction.client;

import resonantinduction.network.RINetwork;

/** Client side of the payloads. Only called from handler bodies that run on the client. */
public final class ClientPayloads {
    private ClientPayloads() {}

    public static void zap(RINetwork.ZapPayload payload) {
        ElectricBolts.add(payload.from(), payload.to(), payload.color());
    }
}
