package hu.javadev.vigszinhaz.service;

import java.io.IOException;

public interface WebPageClient {

  String load(String url) throws IOException;
}
