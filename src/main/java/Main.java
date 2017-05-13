import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.Query;
import org.gephi.filters.api.Range;
import org.gephi.filters.plugin.graph.DegreeRangeBuilder;
import org.gephi.filters.plugin.graph.GiantComponentBuilder;
import org.gephi.filters.plugin.graph.InDegreeRangeBuilder;
import org.gephi.filters.plugin.graph.NeighborsBuilder;
import org.gephi.filters.plugin.operator.INTERSECTIONBuilder;
import org.gephi.graph.api.*;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.spi.GraphExporter;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.EdgeDirectionDefault;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Main main = new Main();
        main.script();
    }
    public void script() {
        //Initialization - create ProjectController
        ProjectController projectController = Lookup.getDefault().lookup(ProjectController.class);
        projectController.newProject();
        Workspace workspace = projectController.getCurrentWorkspace();

        //Get models and controllers for this new workspace - will be useful later
        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
        ImportController importController = Lookup.getDefault().lookup(ImportController.class);
        FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
        AppearanceController appearanceController = Lookup.getDefault().lookup(AppearanceController.class);
        AppearanceModel appearanceModel = appearanceController.getModel();

        //Import file
        Container container;
        try {
            File file = new File(getClass().getResource("/sydney.graphml").toURI());    //Define path to the graph file
            container = importController.importFile(file);
            container.getLoader().setEdgeDefault(EdgeDirectionDefault.DIRECTED);   //Force DIRECTED
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        //Append imported data to GraphAPI
        importController.process(container, new DefaultProcessor(), workspace);
        //See if graph is well imported
        DirectedGraph graph = graphModel.getDirectedGraph();
        System.out.println("Nodes: " + graph.getNodeCount());
        System.out.println("Edges: " + graph.getEdgeCount());

        //Giant Components Filter - Result Nodes: 406398 / Edges: 754550
        GiantComponentBuilder.GiantComponentFilter giantComponentFilter = new GiantComponentBuilder.GiantComponentFilter();
        giantComponentFilter.init(graph);
        Query queryGiantComponent = filterController.createQuery(giantComponentFilter);

        //Degree Filter
        DegreeRangeBuilder.DegreeRangeFilter degreeFilter = new DegreeRangeBuilder.DegreeRangeFilter();
        degreeFilter.init(graph);
        degreeFilter.setRange(new Range(15, Integer.MAX_VALUE));     //Remove nodes with degree < 15
        Query queryDegreeFilter = filterController.createQuery(degreeFilter);

        //Neighbor Network filter
        NeighborsBuilder.NeighborsFilter neighborsFilter = new NeighborsBuilder.NeighborsFilter();
        neighborsFilter.setDepth(1);
        neighborsFilter.setSelf(true);
        Query queryNeighbor = filterController.createQuery(neighborsFilter);

        //In-Degree Filter
        InDegreeRangeBuilder.InDegreeRangeFilter inDegreeRangeFilter = new InDegreeRangeBuilder.InDegreeRangeFilter();
        inDegreeRangeFilter.init(graph);
        inDegreeRangeFilter.setRange(new Range(1,Integer.MAX_VALUE));
        Query queryIndegree = filterController.createQuery(inDegreeRangeFilter);

        filterController.add(queryGiantComponent);
        filterController.add(queryDegreeFilter);
        filterController.add(queryNeighbor);
        filterController.add(queryIndegree);
        filterController.setSubQuery(queryGiantComponent,queryDegreeFilter);

        INTERSECTIONBuilder.IntersectionOperator intersectionOperator = new INTERSECTIONBuilder.IntersectionOperator();
        Query finalQuery = filterController.createQuery(intersectionOperator);
        filterController.setSubQuery(finalQuery,queryGiantComponent);
        filterController.setSubQuery(finalQuery,queryNeighbor);
        GraphView view = filterController.filter(finalQuery);
        graphModel.setVisibleView(view);    //Set the filter result as the visible view

        //Export Status
        UndirectedGraph graphVisible = graphModel.getUndirectedGraphVisible();
        System.out.println("Nodes: " + graphVisible.getNodeCount());
        System.out.println("Edges: " + graphVisible.getEdgeCount());

        //Run YifanHuLayout for 100 passes - The layout always takes the current visible view
        YifanHuLayout layout = new YifanHuLayout(null, new StepDisplacement(1f));
        layout.setGraphModel(graphModel);
        layout.resetPropertiesValues();
        layout.setOptimalDistance(200f);
        layout.initAlgo();

        for (int i = 0; i < 100 && layout.canAlgo(); i++) {
            layout.goAlgo();
        }

        layout.endAlgo();

        ExportController exportController = Lookup.getDefault().lookup(ExportController.class);

        //Export only visible graph
        GraphExporter exporter = (GraphExporter) exportController.getExporter("gexf");     //Get GEXF exporter
        exporter.setExportVisible(true);  //Only exports the visible (filtered) graph
        exporter.setWorkspace(workspace);
        try {
            exportController.exportFile(new File("output.gexf"), exporter);
            System.out.println("Filtered Graph Exported");
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
    }
}
