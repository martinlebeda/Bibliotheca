package bibliotheca.ftmap;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Bits;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 30.5.16
 */
// TODO Lebeda - exprahovat interface a tohle změnit na FTStringMap
// TODO Lebeda - dopsat testy
// TODO Lebeda - dopsat FTAbstractMap
// TODO Lebeda - dopsat FTGenericMap
// TODO Lebeda - dopsat FTIntegerMap
// TODO Lebeda - publikovat na maven repository jako samostatnou knihovnu
public class FTMap implements Map<String, String> {
    private final StandardAnalyzer analyzer;
    private final Path indexDir;
//    private IndexWriter writer = null;
//    private DirectoryReader reader = null;

    // TODO - JavaDoc - Lebeda
    public FTMap(Path indexDir) throws IOException {
        this.indexDir = indexDir;
        analyzer = new StandardAnalyzer();
//        Directory index = FSDirectory.open(indexDir);
    }

    @Override
    public int size() {
        try {
            IndexReader reader = DirectoryReader.open(FSDirectory.open(indexDir));
            final int numDocs = reader.numDocs();
            reader.close();
            return numDocs;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean isEmpty() {
        try {
            IndexReader reader = null;
            reader = DirectoryReader.open(FSDirectory.open(indexDir));
            final boolean b = reader.numDocs() == 0;
            reader.close();
            return b;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        String value = getByKey(key.toString());
        return value != null;
    }

    @Override
    public boolean containsValue(Object value) {
        try {
            Set<String> keys = getByValue(value.toString());
            return !keys.isEmpty();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String get(Object key) {
        return getByKey(key.toString());
    }

    @Override
    public String put(String key, String value) {
        try {
            addDoc(key, value);
            return value;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String remove(Object key) {
        try {
            TermQuery query = new TermQuery(new Term("id", key.toString()));

            final String result = getByKey(key.toString());

            if (result != null) {
                IndexWriter writer = null;
                writer = new IndexWriter(FSDirectory.open(indexDir), getIndexWriterConfig());
                writer.deleteDocuments(query);
                writer.commit();
                writer.close();
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        m.entrySet().stream().forEach(entry -> {
            try {
                addDoc(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Override
    public void clear() {
        try {
            IndexWriter writer = new IndexWriter(FSDirectory.open(indexDir), getIndexWriterConfig());
            writer.deleteAll();
            writer.commit();
            writer.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return entrySet().parallelStream().map(Entry::getKey).collect(Collectors.toSet());
    }

    @NotNull
    @Override
    public Collection<String> values() {
        return entrySet().parallelStream().map(Entry::getValue).collect(Collectors.toSet());
    }

    @NotNull
    @Override
    public Set<Entry<String, String>> entrySet() {
        try {
            final HashSet<Entry<String, String>> entries = new HashSet<>();
            IndexReader reader = DirectoryReader.open(FSDirectory.open(indexDir));
            Bits liveDocs = MultiFields.getLiveDocs(reader);
            for (int i=0; i<reader.maxDoc(); i++) {
                if (liveDocs != null && !liveDocs.get(i))
                    continue;

                Document d = reader.document(i);
                entries.add(new AbstractMap.SimpleEntry<>(d.get("id"), d.get("value"))); // TODO Lebeda - constants for field names
            }
            reader.close();
            return entries;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // TODO - JavaDoc - Lebeda
    public Set<String>
    search(final String queryStr, final int maxDocs) throws ParseException, IOException {
        final QueryParser queryParser = new QueryParser("value", analyzer);
        if (queryStr.matches("^[*?].*")) {
            queryParser.setAllowLeadingWildcard(true);
        }
        Query q = queryParser.parse(queryStr);

        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexDir));
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs docs = searcher.search(q, maxDocs);

        ScoreDoc[] hits = docs.scoreDocs;

        Set<String> result = new HashSet<>();
        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            result.add(d.get("id"));
            System.out.println((i + 1) + ". " + d.get("id") + "\t" + d.get("value"));
        }
        reader.close();
        return result;
    }

    private String getByKey(String key) {
        try {
            final FSDirectory fsDirectory;
            fsDirectory = FSDirectory.open(indexDir);
            IndexReader reader = DirectoryReader.open(fsDirectory);
            IndexSearcher searcher = new IndexSearcher(reader);
            TermQuery query = new TermQuery(new Term("id", key));
            final TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);

            String result = null;
            if (topDocs.scoreDocs.length == 1) {
                int docId = topDocs.scoreDocs[0].doc;
                Document d = searcher.doc(docId);
                result = d.get("value");
            }
            reader.close();

            return result;
        } catch (IndexNotFoundException e) {
            return null;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Set<String> getByValue(String key) throws IOException {
        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexDir));
        IndexSearcher searcher = new IndexSearcher(reader);
        TermQuery query = new TermQuery(new Term("value", key));
        final TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
        ScoreDoc[] hits = topDocs.scoreDocs;

        Set<String> result = new HashSet<>();
        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            result.add(d.get("id"));
        }
        reader.close();

        return result;
    }


    // TODO - JavaDoc - Lebeda
    synchronized private void addDoc(String key, String value) throws IOException, InterruptedException {
        Document doc = new Document();
        doc.add(new StringField("id", key, Field.Store.YES));
        doc.add(new TextField("value", value, Field.Store.YES));

        System.out.println("indexing " + key + " - " + value);

        while (true) {
            try {
                IndexWriter writer = new IndexWriter(FSDirectory.open(indexDir), getIndexWriterConfig());
                if (containsKey(key)) {
                    writer.updateDocument(new Term("id", key), doc);
                } else {
                    writer.addDocument(doc);
                }
                writer.commit();
                writer.close();
                break;
            } catch (LockObtainFailedException e) {
                System.out.println("except on " + key + " - " + value);
//                System.gc();
                Thread.sleep(100);
            }
        }
    }

//    @NotNull
//    private DirectoryReader DirectoryReader.open(FSDirectory.open(indexDir)) throws IOException {
//        if (writer != null) {
//            writer.commit();
//            writer.close();
//            writer = null;
//        }
//
//        if (reader == null) {
//            reader = DirectoryReader.open(FSDirectory.open(indexDir));
//        } else {
//            System.out.println("ahoj");
//        }
//        return reader;
//    }

//    @NotNull
//    private IndexWriter getWriter() throws IOException {
//        if (reader != null) {
//            reader.close();
//            reader = null;
//        }
//
//        if (writer == null) {
//            writer = new IndexWriter(FSDirectory.open(indexDir), getIndexWriterConfig());
//        }
//        return writer;
//    }

    // TODO - JavaDoc - Lebeda
//    public void close() throws IOException {
//        if (writer != null) {
//            writer.commit();
//            writer.close();
//        }
//        if (reader != null) {
//            reader.close();
//        }
//    }

    @NotNull
    private IndexWriterConfig getIndexWriterConfig() {
        return new IndexWriterConfig(analyzer);
    }

}
