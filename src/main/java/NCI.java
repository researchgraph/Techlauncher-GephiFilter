/**
 * Created by wangkun on 15/05/2017.
 */
/* Ref: https://github.com/gephi/gephi/wiki/How-to-code-with-the-Toolkit */

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.appearance.api.Function;
import org.gephi.appearance.plugin.RankingElementColorTransformer;
import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.Query;
import org.gephi.filters.api.Range;
import org.gephi.filters.plugin.graph.DegreeRangeBuilder;
import org.gephi.filters.plugin.graph.DegreeRangeBuilder.DegreeRangeFilter;
import org.gephi.filters.plugin.graph.GiantComponentBuilder;
import org.gephi.filters.plugin.graph.InDegreeRangeBuilder;
import org.gephi.filters.plugin.graph.NeighborsBuilder;
import org.gephi.filters.plugin.operator.INTERSECTIONBuilder;
import org.gephi.filters.plugin.partition.PartitionCountBuilder;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.UndirectedGraph;
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
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.statistics.plugin.GraphDistance;
import org.openide.util.Lookup;
import org.gephi.appearance.api.*;

public class NCI {

    public static void main(String[] args) {
        NCI hs = new NCI();
        hs.script();

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
            //Define path to the graph file
            File file = new File(getClass().getResource("/NCI.graphml").toURI());
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




        //Giant Components Filter
        GiantComponentBuilder.GiantComponentFilter giantComponentFilter = new GiantComponentBuilder.GiantComponentFilter();
        giantComponentFilter.init(graph);
        Query queryGiantComponent = filterController.createQuery(giantComponentFilter);

        //Degree Filter
        DegreeRangeBuilder.DegreeRangeFilter degreeFilter = new DegreeRangeBuilder.DegreeRangeFilter();
        degreeFilter.init(graph);
        degreeFilter.setRange(new Range(3, Integer.MAX_VALUE));     //Remove nodes with degree < 3
        Query queryDegreeFilter = filterController.createQuery(degreeFilter);



        filterController.add(queryGiantComponent);
        filterController.add(queryDegreeFilter);
        filterController.setSubQuery(queryGiantComponent,queryDegreeFilter);

        INTERSECTIONBuilder.IntersectionOperator intersectionOperator = new INTERSECTIONBuilder.IntersectionOperator();
        Query finalQuery = filterController.createQuery(intersectionOperator);
        filterController.setSubQuery(finalQuery,queryGiantComponent);
        GraphView view = filterController.filter(finalQuery);
        graphModel.setVisibleView(view);    //Set the filter result as the visible view




        /*

        //Giant Filter
        GiantComponentBuilder.GiantComponentFilter giantComponentFilter = new GiantComponentBuilder
                .GiantComponentFilter();
        giantComponentFilter.init(graph);
        Query queryGiantComponent = filterController.createQuery(giantComponentFilter);
        GraphView view = filterController.filter(queryGiantComponent);
        graphModel.setVisibleView(view);    //Set the filter result as the visible view

        //Partition Count Filter

        PartitionCountBuilder.PartitionCountFilter partitionCountFilter = new PartitionCountBuilder.PartitionCountFilter(graphModel.getNodeTable().getColumn("published_date") ,appearanceModel );
        partitionCountFilter.init(graph);
        partitionCountFilter.setRange(new Range(180, 300));
        System.out.println(partitionCountFilter.getRange());
        System.out.println(graphModel.getNodeTable().getColumn("published_date"));
        Query queryPartitionCountFilter = filterController.createQuery(partitionCountFilter);
        System.out.println(queryGiantComponent.toString());
        GraphView view1 = filterController.filter(queryPartitionCountFilter);
        graphModel.setVisibleView(view1);

        //Neighbor Network filter
        NeighborsBuilder.NeighborsFilter neighborsFilter = new NeighborsBuilder.NeighborsFilter();
        neighborsFilter.setDepth(1);
        neighborsFilter.setSelf(true);
        Query queryNeighbor = filterController.createQuery(neighborsFilter);
        GraphView view2 = filterController.filter(queryNeighbor);
        graphModel.setVisibleView(view2);


        filterController.add(queryGiantComponent);
        filterController.add(queryPartitionCountFilter);
        filterController.add(queryNeighbor);
        //filterController.setSubQuery(queryGiantComponent,queryPartitionCountFilter);


        INTERSECTIONBuilder.IntersectionOperator intersectionOperator = new INTERSECTIONBuilder.IntersectionOperator();
        Query finalQuery = filterController.createQuery(intersectionOperator);
        filterController.setSubQuery(finalQuery,queryGiantComponent);
        filterController.setSubQuery(finalQuery,queryNeighbor);
        GraphView finalview = filterController.filter(finalQuery);
        graphModel.setVisibleView(finalview);    //Set the filter result as the visible view


        */

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

        //Get Centrality
//        GraphDistance distance = new GraphDistance();
//        distance.setDirected(true);
//        distance.execute(graphModel);

        //Define Output Preview
//        model.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
//        model.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(Color.RED));
//        model.getProperties().putValue(PreviewProperty.EDGE_THICKNESS, new Float(0.1f));
//        model.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, model.getProperties().getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(8));

        //Export file
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        GraphExporter exporter = (GraphExporter) ec.getExporter("gexf");     //Get GEXF exporter
        exporter.setExportVisible(true);  //Only exports the visible (filtered) graph
        exporter.setWorkspace(workspace);
        try {
            ec.exportFile(new File("output.gexf"), exporter);
            System.out.println("Filtered Graph Exported");
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
    }
}
