package com.calibre90.astestmob.obd;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Bounded, read-only TCP discovery on the explicitly selected Android Wi-Fi network. */
public final class ObdWifiDiscovery {
  public static final class Endpoint {
    public final String host;
    public final int port;
    public Endpoint(String host, int port) { this.host=host; this.port=port; }
    @Override public String toString() { return host+":"+port; }
  }
  public static final class Result {
    public final List<Endpoint> open;
    public final String gateway;
    Result(List<Endpoint> open, String gateway) { this.open=open; this.gateway=gateway; }
  }
  private ObdWifiDiscovery() {}

  public static Result scan(Context context) throws IOException {
    ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
    if(cm==null)throw new IOException("Сетевой сервис Android недоступен");
    Network wifi=null;
    LinkProperties props=null;
    for(Network n:cm.getAllNetworks()) {
      NetworkCapabilities caps=cm.getNetworkCapabilities(n);
      if(caps!=null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)\n          && !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
        wifi=n;props=cm.getLinkProperties(n);break;
      }
    }
    if(wifi==null || props==null)throw new IOException("Подключитесь к Wi-Fi диагностического прибора");
    LinkedHashSet<String> hosts=new LinkedHashSet<>();
    String gateway=null;
    for(android.net.RouteInfo route:props.getRoutes()) {
      if(route.isDefaultRoute() && route.getGateway() instanceof Inet4Address\n          && !route.getGateway().isAnyLocalAddress()) {
        gateway=route.getGateway().getHostAddress();
        hosts.add(gateway);
      }
    }
    // Common direct-connection ELM Wi-Fi address; only attempt if on the same /24.
    for(LinkAddress addr:props.getLinkAddresses()) {
      if(addr.getAddress() instanceof Inet4Address\n          && !addr.getAddress().isAnyLocalAddress()\n          && !addr.getAddress().isLoopbackAddress()) {
        byte[] b=addr.getAddress().getAddress();
        String subnet=(b[0]&255)+"."+(b[1]&255)+"."+(b[2]&255)+".";
        hosts.add(subnet+"1");
        hosts.add(subnet+"10");
      }
    }
    if(hosts.isEmpty()) throw new IOException("У Wi-Fi нет подходящего IPv4-адреса или шлюза");\n    int[] ports={35000, 23, 2000, 2001};
    List<Endpoint> open=new ArrayList<>();
    for(String host:hosts) {
      for(int port:ports) {
        try(Socket socket=wifi.getSocketFactory().createSocket()) {
          socket.connect(new InetSocketAddress(host,port),350);
          open.add(new Endpoint(host,port));
        } catch(IOException ignored) { /* closed or filtered port */ }
      }
    }
    return new Result(open,gateway);
  }
}
