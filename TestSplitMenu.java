import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Parent;

public class TestSplitMenu extends Application {
    @Override
    public void start(Stage stage) {
        SplitMenuButton btn = new SplitMenuButton();
        btn.setText("Test");
        
        VBox root = new VBox(btn);
        Scene scene = new Scene(root, 200, 200);
        stage.setScene(scene);
        stage.show();
        
        System.out.println("Hierarchy:");
        printHierarchy(btn, 0);
        
        System.exit(0);
    }
    
    private void printHierarchy(Node node, int level) {
        for(int i=0; i<level; i++) System.out.print("  ");
        System.out.println(node.getClass().getSimpleName() + " (styleClass: " + node.getStyleClass() + ")");
        if (node instanceof Parent) {
            for (Node child : ((Parent)node).getChildrenUnmodifiable()) {
                printHierarchy(child, level + 1);
            }
        }
    }
}
