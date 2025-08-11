package com.sumitsee.backend.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class IndexController {
  private final List<String> corpus = new CopyOnWriteArrayList<>();
  private final String version = "v1";

  @GetMapping("/version")
  public Map<String,Object> version() { return Map.of("service","shadow-index-demo","version",version); }

  // Simulate indexing: add doc
  @PostMapping("/docs")
  public Map<String,Object> add(@RequestBody Map<String,String> body) {
    String text = body.getOrDefault("text","");
    corpus.add(text);
    return Map.of("version",version,"added",text,"size",corpus.size());
  }

  // Clear index
  @DeleteMapping("/docs")
  public Map<String,Object> clear() { corpus.clear(); return Map.of("version",version,"size",corpus.size()); }

  // Search by substring contains
  @GetMapping("/search")
  public Map<String,Object> search(@RequestParam("q") String q) {
    List<String> hits = corpus.stream().filter(s -> s.toLowerCase().contains(q.toLowerCase())).collect(Collectors.toList());
    return Map.of("version",version,"query",q,"hits",hits);
  }

  // Inspect index
  @GetMapping("/index")
  public Map<String,Object> index() {
    return Map.of("version",version,"size",corpus.size(),"docs",corpus);
  }
}
