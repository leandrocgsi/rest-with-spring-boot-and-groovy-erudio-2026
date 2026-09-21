package br.com.erudio.testsupport

final class NetworkAssumptions {

    private static final String REPORT_IMAGES_HOST = 'raw.githubusercontent.com'

    private NetworkAssumptions() {}

    static boolean reportImagesAreReachable() {
        isReachable(REPORT_IMAGES_HOST, 443)
    }

    private static boolean isReachable(String host, int port) {
        try {
            new Socket().withCloseable { Socket socket ->
                socket.connect(new InetSocketAddress(host, port), 3000)
                true
            }
        } catch (IOException ignored) {
            false
        }
    }
}
