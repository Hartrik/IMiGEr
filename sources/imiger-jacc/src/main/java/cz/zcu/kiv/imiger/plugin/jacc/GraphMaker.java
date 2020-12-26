package cz.zcu.kiv.imiger.plugin.jacc;

import cz.zcu.kiv.ccu.ApiCmpStateResult;
import cz.zcu.kiv.ccu.ApiInterCompatibilityResult;
import cz.zcu.kiv.imiger.vo.*;
import cz.zcu.kiv.jacc.cmp.JComparator;
import cz.zcu.kiv.jacc.javatypes.*;
import cz.zcu.kiv.typescmp.CmpResult;
import cz.zcu.kiv.typescmp.CmpResultNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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
    private int level;
    private String compInfoInnerJSON;

    private ApiInterCompatibilityResult comparisonResult;
    private File[] uploadedFiles;
    private Set<String> origins;
    private Properties jaccMessages;

    private static final File NOT_FOUND = null;

    public GraphMaker(ApiInterCompatibilityResult comparisonResult, File[] uploadedFiles) {
        this.graph = new Graph();
        graph.setVertexArchetypes(Collections.singletonList(new VertexArchetype(VERTEX_ARCHETYPE_NAME, ARCHETYPE_TEXT)));
        graph.setEdgeArchetypes(Collections.singletonList(new EdgeArchetype(EDGE_ARCHETYPE_NAME, ARCHETYPE_TEXT)));

        this.uploadedFiles = uploadedFiles;
        this.origins = comparisonResult.getOriginsImportingIncompatibilities();
        this.comparisonResult = comparisonResult;

        jaccMessages = new Properties();
        InputStream in = null;
        try {
            in = JComparator.class.getResourceAsStream("messages.properties");
            jaccMessages.load(in);
        } catch (IOException e) {
            logger.trace(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.trace(e);
                }
            }
        }
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

            Set<CmpResult<File>> result = new HashSet<CmpResult<File>>();
            String compInfoJSON = "";
            this.compInfoInnerJSON = "";
            String compInfoJSONNF = "";
            String NFClassName = "";
            File firstOrigin = null;
            File secondOrigin = null;
            File firstOriginNF = null;
            File secondOriginNF = null;
            List<String> NFClasses = new ArrayList<String>();
            for (String origin : this.origins) { // cyklus pres (ne)kompatibilni JAR
                incompatibleClasses = comparisonResult.getClassesImportingIncompatibilities(origin);

                for (JClass incompatibleClass : incompatibleClasses) {
                    apiCmpResult = comparisonResult.getIncompatibleResults(incompatibleClass, origin);
                    for (ApiCmpStateResult apiCmpResult1 : apiCmpResult) {
                        if (apiCmpResult1.getResult().getSecondObject() != null) {
                            result.add(apiCmpResult1.getResult());

                            List<CmpResultNode> children = apiCmpResult1.getResult().getChildren();

                            this.level = 0;
                            this.compInfoInnerJSON += "";
                            this.findCompatibilityCause(children, incompatibleClass.getName(), apiCmpResult1.getResult().getSecondObject().getName(), "");
                            this.compInfoInnerJSON += ",";
                            if (firstOrigin == null && secondOrigin == null) {
                                firstOrigin = apiCmpResult1.getResult().getFirstObject();
                                secondOrigin = apiCmpResult1.getResult().getSecondObject();
                            }
                        } else {
                            NFClassName = apiCmpResult1.getResult().getFirstObject().getName();
                            if (!NFClassName.equals("") && !NFClasses.contains(NFClassName)) {
                                NFClasses.add(NFClassName);
                                compInfoJSONNF += "{theClass: \"" + NFClassName + "\", incomps: [ ";
                                compInfoJSONNF += "]},";
                            }

                            if (firstOriginNF == null && secondOriginNF == null) {
                                firstOriginNF = apiCmpResult1.getResult().getFirstObject();
                                secondOriginNF = null;
                            }
                        }

                    }

                    if (!this.compInfoInnerJSON.equals(",") && !this.compInfoInnerJSON.equals("")) {
                        compInfoJSON += "{theClass: \"" + incompatibleClass.getName() + "\", incomps: [ ";
                        compInfoJSON += this.compInfoInnerJSON;
                        compInfoJSON += "]},";
                    }
                    this.compInfoInnerJSON = "";
                    NFClassName = "";

                }
                NFClasses.clear();

                if (firstOrigin != null) {
                    id++;
                    edge = new Edge(id, vertexId(createSymbolicName(firstOrigin)), vertexId(createSymbolicName(secondOrigin)), "",
                            Collections.singletonList(new SubedgeInfo(id, DEFAULT_ARCHETYPE_INDEX, Collections.singletonList(new String[]{"compInfo", "[" + compInfoJSON + "]"}))));
                    this.graph.getEdges().add(edge);
                }

                if (firstOriginNF != null) {
                    id++;
                    edge = new Edge(id, vertexId(createSymbolicName(secondOriginNF)), vertexId(createSymbolicName(firstOriginNF)), "",
                            Collections.singletonList(new SubedgeInfo(id, DEFAULT_ARCHETYPE_INDEX, Collections.singletonList(new String[]{"compInfo", "[" + compInfoJSONNF + "]"}))));
                    this.graph.getEdges().add(edge);
                }

                firstOrigin = null;
                secondOrigin = null;
                firstOriginNF = null;
                secondOriginNF = null;
                compInfoJSON = "";
                compInfoJSONNF = "";

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

    /**
     * Recursive function for traversing tree with incompatibility information
     * Creates JSON string
     *
     * @param children
     * @param className
     * @param jarName
     * @param corrStrategy
     */
    public void findCompatibilityCause(List<CmpResultNode> children, String className, String jarName, String corrStrategy) {
        for (CmpResultNode child : children) {
            Object o = child.getResult().getFirstObject();
            this.compInfoInnerJSON += "{desc: {level: \"" + String.valueOf(this.level);

            if (child.isIncompatibilityCause()) {

                if (o instanceof HasName) {
                    this.compInfoInnerJSON += "\", name: \"" + this.jaccMessages.getProperty(child.getContentCode()) + ": " + ((HasName) o).getName();

                    this.compInfoInnerJSON
                            += (!child.getResult().getInherentDiff().name().equals("DEL") ? "\", objectNameFirst: \"" + ((HasName) o).getName() : "")
                            + (!child.getResult().getInherentDiff().name().equals("DEL") ? "\", objectNameSecond: \"" + ((HasName) child.getResult().getSecondObject()).getName() : "");
                } else {
                    if (o instanceof JMethod) {
                        this.compInfoInnerJSON += "\", name: \"M " + getCompleteMethodName(o);
                    } else {
                        this.compInfoInnerJSON += "\", name: \"" + this.jaccMessages.getProperty(child.getContentCode());
                    }

                    this.compInfoInnerJSON
                            += (!child.getResult().getInherentDiff().name().equals("DEL") ? "\", objectNameFirst: \"" + o.toString() : "")
                            + (!child.getResult().getInherentDiff().name().equals("DEL") ? "\", objectNameSecond: \"" + child.getResult().getSecondObject().toString() : "");

                }

                this.compInfoInnerJSON += "\", className: \"" + className;
                this.compInfoInnerJSON += "\", jarName: \"" + jarName;

                String incompName = this.getIncompatibilityName(child, corrStrategy);
                if (incompName.equals("")) {
                    this.compInfoInnerJSON += "\", incompName: \"Incompatible " + this.jaccMessages.getProperty(child.getContentCode()) + " -> " + corrStrategy; //child.getResult().getStrategy().name();
                } else {
                    this.compInfoInnerJSON += "\", incompName: \"" + incompName + (child.getResult().getInherentDiff().name().equals("DEL") ? " is missing -> " + child.getResult().getStrategy().name() : "");
                }

                this.compInfoInnerJSON += "\", isIncompCause: \"" + String.valueOf(child.isIncompatibilityCause())
                        + "\", strategy: \"" + child.getResult().getStrategy().name()
                        + "\", difference: \"" + child.getResult().getInherentDiff().name() + "\"}, subtree:";

            } else {
                if (o instanceof JMethod) {
                    this.compInfoInnerJSON += "\", name: \"<span class='entity'>M</span> " + getCompleteMethodName(o);
                } else if (o instanceof JField) {
                    this.compInfoInnerJSON += "\", name: \"<span class='entity'>F</span> " + getShortName2(o.toString());

                } else if (o instanceof HasName) {
                    this.compInfoInnerJSON += "\", name: \"" + this.jaccMessages.getProperty(child.getContentCode()) + ": " + ((HasName) o).getName();
                } else {
                    this.compInfoInnerJSON += "\", name: \"" + this.jaccMessages.getProperty(child.getContentCode());
                }

                this.compInfoInnerJSON += "\", isIncompCause: \"" + String.valueOf(child.isIncompatibilityCause()) + "\"}, subtree: ";

            }
            if (!child.getResult().childrenCompatible()) {
                String strategy = "";
                if (this.level == 1) {
                    strategy = child.getResult().getStrategy().name();
                }
                this.level++;
                this.compInfoInnerJSON += "[";
                // tady se podivat na child.getResult()
                this.findCompatibilityCause(child.getResult().getChildren(), className, jarName, strategy);
                this.level--;
                this.compInfoInnerJSON += "],";
            } else {
                this.compInfoInnerJSON += "[],";
            }
            this.compInfoInnerJSON += "},";
        }

    }

    private String getShortName(String longName) {
        return longName.substring(longName.lastIndexOf('.') + 1);
    }

    private String getShortName2(String longName) {
        return longName.substring(longName.lastIndexOf(':') + 1);
    }

    private String getCompleteMethodName(Object o) {
        String methodName = "";
        methodName += getShortName(((JMethod) o).getReturnType().getName());
        methodName += " " + ((JMethod) o).getName();
        methodName += " (";
        for (JType type : ((JMethod) o).getParameterTypes()) {
            methodName += getShortName(type.getName()) + ", ";
        }
        methodName = methodName.replaceAll(", $", "");
        methodName += ")";
        return methodName;
    }

    private String getFieldName(Object o) {
        return ((JField) o).getType().getName() + " " + ((JField) o).getName();
    }

    private String getIncompatibilityName(CmpResultNode child, String corrStrategy) {
        Object o = child.getResult().getFirstObject();

        String incompName = "";
        switch (child.getContentCode()) {
            case "cmp.child.method.return.type":
                incompName = this.jaccMessages.getProperty(child.getContentCode()) + " different -> " + corrStrategy;
                break;
            case "cmp.child.method.param.type": {
                if (o instanceof HasName) {
                    incompName = "Parameter " + getShortName(((HasName) o).getName()) + " different -> " + corrStrategy;
                } else {
                    incompName = "Parameter " + getShortName(o.toString()) + " different -> " + corrStrategy;
                }
            }
            break;
            case "cmp.child.method.invocation":
                incompName = "Invoke Virtual" + " -> " + child.getResult().getStrategy().name();
                break;
            case "cmp.child.method":
                incompName = "<span class='entity'>M</span> " + getCompleteMethodName(o);
                break;
            case "cmp.child.constructor":
                incompName = "<span class='entity'>C</span> " + getCompleteMethodName(o);
                break;
            case "cmp.child.field":
                incompName = "<span class='entity'>F</span> " + getFieldName(o);
                break;
            case "cmp.child.modifier":
                incompName = "<span class='entity'>P</span> " + o.toString() + " -> " + corrStrategy;
                break;
            default:
                incompName = "";
                break;
        }
        return incompName;
    }

}
