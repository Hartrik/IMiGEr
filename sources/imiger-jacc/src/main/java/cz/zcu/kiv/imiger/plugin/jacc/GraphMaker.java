package cz.zcu.kiv.imiger.plugin.jacc;

import cz.zcu.kiv.ccu.ApiCmpStateResult;
import cz.zcu.kiv.ccu.ApiInterCompatibilityResult;
import cz.zcu.kiv.imiger.vo.*;
import cz.zcu.kiv.jacc.javatypes.JClass;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

// From cocaex, only custom graph interface,
/**
 * This class provides creation graph of the loaded bundle.
 *
 * @author Jindra Pavlíková <jindra.pav2@seznam.cz>
 */
public class GraphMaker {

    private static final String VERTEX_ARCHETYPE_NAME = "Vertex";
    private static final String EDGE_ARCHETYPE_NAME = "Edge";
    private static final String ARCHETYPE_TEXT = "";
    private static final int DEFAULT_ARCHETYPE_INDEX = 0;

    private Graph graph;
    private Map<String, Vertex> vertices = new LinkedHashMap<>();
    private Logger logger = Logger.getLogger(GraphMaker.class);

    private ApiInterCompatibilityResult comparisonResult;
    private File[] uploadedFiles;
    private Set<String> origins;

    private static final File NOT_FOUND = null;

    public GraphMaker(ApiInterCompatibilityResult comparisonResult, File[] uploadedFiles) {
        this.graph = new Graph();
        graph.setVertexArchetypes(Collections.singletonList(new VertexArchetype(VERTEX_ARCHETYPE_NAME, ARCHETYPE_TEXT)));
        graph.setEdgeArchetypes(Collections.singletonList(new EdgeArchetype(EDGE_ARCHETYPE_NAME, ARCHETYPE_TEXT)));

        this.uploadedFiles = uploadedFiles;
        this.origins = comparisonResult.getOriginsImportingIncompatibilities();
        this.comparisonResult = comparisonResult;
    }

    /**
     * This method creates the vertices and adds them to the graph.
     */
    private void generateVertices() {
        Vertex vertex;
        String name;
        String symbolicName;
        int id = 0;

        for (File origin : this.uploadedFiles) {
            symbolicName = createSymbolicName(origin);
            name = origin.getName();
            vertex = new Vertex(id++, symbolicName, DEFAULT_ARCHETYPE_INDEX, name, Collections.emptyList());
            this.graph.getVertices().add(vertex);
            this.vertices.put(symbolicName, vertex);
        }

        symbolicName = createSymbolicName(NOT_FOUND);
        name = "NOT_FOUND";
        vertex = new Vertex(id++, symbolicName, DEFAULT_ARCHETYPE_INDEX, name, Collections.emptyList());
        this.graph.getVertices().add(vertex);
        this.vertices.put(symbolicName, vertex);
    }

    /**
     * This method creates the edges and adds them to the graph.
     */
    private void createEdges() throws Exception{
        Set<JClass> incompatibleClasses;
        Set<ApiCmpStateResult> apiCmpResult;

        Edge edge;
        int id = 0;

        // compatibility checking START
        try {

            String NFClassName = "";
            File firstOrigin = null;
            File secondOrigin = null;
            File firstOriginNF = null;
            File secondOriginNF = null;
            List<String> NFClasses = new ArrayList<String>();
            for (String origin : this.origins) {
                incompatibleClasses = comparisonResult.getClassesImportingIncompatibilities(origin);

                for (JClass incompatibleClass : incompatibleClasses) {
                    apiCmpResult = comparisonResult.getIncompatibleResults(incompatibleClass, origin);
                    for (ApiCmpStateResult apiCmpResult1 : apiCmpResult) {
                        if (apiCmpResult1.getResult().getSecondObject() != null) {

                            if (firstOrigin == null && secondOrigin == null) {
                                firstOrigin = apiCmpResult1.getResult().getFirstObject();
                                secondOrigin = apiCmpResult1.getResult().getSecondObject();
                            }
                        } else {
                            NFClassName = apiCmpResult1.getResult().getFirstObject().getName();
                            if (!NFClassName.equals("") && !NFClasses.contains(NFClassName)) {
                                NFClasses.add(NFClassName);
                            }

                            if (firstOriginNF == null) {
                                firstOriginNF = apiCmpResult1.getResult().getFirstObject();
                                secondOriginNF = null;
                            }
                        }
                    }
                }

                String incomp = incompatibleClasses.stream().map(JClass::getShortName).distinct().collect(Collectors.joining(", ", "[", "]"));
                String notFound = NFClasses.stream().collect(Collectors.joining(", ", "[", "]"));
                List<String[]> attributes = Collections.singletonList(
                        new String[]{"compInfo", "incompatibleClasses: " + incomp + ", classesNotFound: " + notFound});
                NFClasses.clear();

                if (firstOrigin != null) {
                    id++;
                    edge = new Edge(id, vertexId(createSymbolicName(firstOrigin)), vertexId(createSymbolicName(secondOrigin)), "",
                            Collections.singletonList(new SubedgeInfo(id, DEFAULT_ARCHETYPE_INDEX, attributes)));
                    this.graph.getEdges().add(edge);
                }

                if (firstOriginNF != null) {
                    id++;
                    edge = new Edge(id, vertexId(createSymbolicName(secondOriginNF)), vertexId(createSymbolicName(firstOriginNF)), "",
                            Collections.singletonList(new SubedgeInfo(id, DEFAULT_ARCHETYPE_INDEX, attributes)));
                    this.graph.getEdges().add(edge);
                }

                firstOrigin = null;
                secondOrigin = null;
                firstOriginNF = null;
                secondOriginNF = null;

            }
        } catch (Exception e) {
            System.err.println("ERROR createEdges()");
            System.err.println(e.toString());
            logger.trace(e);
            throw new Exception(e);
        }
        // compatibility checking END
    }

    private int vertexId(String symbolicName) {
        Vertex vertex = vertices.get(symbolicName);
        return vertex.getId();
    }

    /**
     * Create symbolic name.
     *
     * @param origin
     * @return Symbolic name.
     */
    private String createSymbolicName(File origin) {
        if (origin == null) {
            return "NOT_FOUND";
        } else {
            return origin.getName();
        }
    }

    /**
     * Generates the graph with vertices and edges.
     *
     * @return graph
     */
    public Graph generate()throws Exception {
        this.generateVertices();
        try {
            this.createEdges();
        } catch (Exception e) {
            logger.trace(e);
            throw new Exception(e);
        }
        return this.graph;
    }

}
