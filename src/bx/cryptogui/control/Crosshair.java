package bx.cryptogui.control;

import javafx.beans.DefaultProperty;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.*;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;



public class Crosshair extends Region {

    private final Line hLine = new Line();
    private final Line vLine = new Line();
    private final Label info = new Label();
    private final HBox infoPane = new HBox(info);
    private final Label hLabel = new Label();
    private final Label vLabel = new Label();

    /**
     * Reactive to crosshair x and y.
     */
    private ReadOnlyBooleanWrapper reactive = new ReadOnlyBooleanWrapper(this, "reactive", false);
    public final boolean isReactive() {
        return reactive.get();
    }
    public final ReadOnlyBooleanProperty reactiveProperty() {
        return reactive.getReadOnlyProperty();
    }

    private DoubleProperty crosshairX = new SimpleDoubleProperty(this, "crosshairX");
    public final double getCrosshairX() {
        return crosshairX.get();
    }
    public final void setCrosshairX(double value) {
        crosshairX.set(value);
    }
    public final DoubleProperty crosshairXProperty() {
        return crosshairX;
    }

    private DoubleProperty crosshairY = new SimpleDoubleProperty(this, "crosshairY");
    public final double getCrosshairY() {
        return crosshairY.get();
    }
    public final void setCrosshairY(double value) {
        crosshairY.set(value);
    }
    public final DoubleProperty crosshairYProperty() {
        return crosshairY;
    }

    private StringProperty infoText = new SimpleStringProperty(this, "infoText");
    public final String getInfoText() {
        return infoText.get();
    }
    public final void setInfoText(String text) {
        infoText.set(text);
    }
    public final StringProperty infoTextProperty() {
        return infoText;
    }

    private ObjectProperty<Pos> infoAlignment = new SimpleObjectProperty<Pos>(this, "infoAlignment", Pos.TOP_LEFT);
    public final Pos getInfoAlignment() {
        return infoAlignment.get();
    }
    public final void setInfoAlignment(Pos value) {
        infoAlignment.set(value);
    }
    public final ObjectProperty<Pos> infoAlignmentProperty() {
        return infoAlignment;
    }

    private ObjectProperty<Insets> infoPadding = new SimpleObjectProperty<>(this, "infoPadding", new Insets(4));
    public final Insets getInfoPadding() {
        return infoPadding.get();
    }
    public final void setInfoPadding(Insets value) {
        infoPadding.set(value);
    }
    public final ObjectProperty<Insets> infoPaddingProperty() {
        return infoPadding;
    }

    private StringProperty vLabelText = new SimpleStringProperty(this, "vLabelText");
    public final String getVLabelText() {
        return vLabelText.get();
    }
    public final void setVLabelText(String text) {
        vLabelText.set(text);
    }
    public final StringProperty vLabelTextProperty() {
        return vLabelText;
    }

    private StringProperty hLabelText = new SimpleStringProperty(this, "hLabelText");
    public final String getHLabelText() {
        return hLabelText.get();
    }
    public final void setHLabelText(String text) {
        hLabelText.set(text);
    }
    public final StringProperty hLabelTextProperty() {
        return hLabelText;
    }

    private ObjectProperty<Side> hLabelSide = new SimpleObjectProperty<>(this, "hLabelSide", Side.LEFT);
    public final Side getHLabelSide() {
        return hLabelSide.get();
    }
    public final void setHLabelSide(Side value) {
        hLabelSide.set(value);
    }
    public final ObjectProperty<Side> hLabelSideProperty() {
        return hLabelSide;
    }

    private ObjectProperty<Side> vLabelSide = new SimpleObjectProperty<>(this, "vLabelSide", Side.BOTTOM);
    public final Side getVLabelSide() {
        return vLabelSide.get();
    }
    public final void setVLabelSide(Side value) {
        vLabelSide.set(value);
    }
    public final ObjectProperty<Side> vLabelSideProperty() {
        return vLabelSide;
    }

    private DoubleProperty hLabelOffset = new SimpleDoubleProperty(this, "hLabelOffset", 0);
    public final double getHLabelOffset() {
        return hLabelOffset.get();
    }
    public final void setHLabelOffset(double value) {
        hLabelOffset.set(value);
    }
    public final DoubleProperty hLabelOffsetProperty() {
        return hLabelOffset;
    }

    private DoubleProperty vLabelOffset = new SimpleDoubleProperty(this, "vLabelOffset", 0);
    public final double getVLabelOffset() {
        return vLabelOffset.get();
    }
    public final void setVLabelOffset(double value) {
        vLabelOffset.set(value);
    }
    public final DoubleProperty vLabelOffsetProperty() {
        return vLabelOffset;
    }

    private ObjectProperty<Insets> hLabelPadding = new SimpleObjectProperty<>(this, "hLabelPadding", new Insets(4));
    public final Insets getHLabelPadding() {
        return hLabelPadding.get();
    }
    public final void setHLabelPadding(Insets value) {
        hLabelPadding.set(value);
    }
    public final ObjectProperty<Insets> hLabelPaddingProperty() {
        return hLabelPadding;
    }

    private ObjectProperty<Insets> vLabelPadding = new SimpleObjectProperty<>(this, "vLabelPadding", new Insets(4));
    public final Insets getVLabelPadding() {
        return vLabelPadding.get();
    }
    public final void setVLabelPadding(Insets value) {
        vLabelPadding.set(value);
    }
    public final ObjectProperty<Insets> vLabelPaddingProperty() {
        return vLabelPadding;
    }

    private ObjectProperty<EventHandler<MouseEvent>> mouseMovedHandler =
            new SimpleObjectProperty<>(this, "mouseMovedHandler", event -> {
                setCrosshairX(event.getX());
                setCrosshairY(event.getY());
            });
    public final EventHandler<MouseEvent> getMouseMovedHandler() {
        return mouseMovedHandler.get();
    }
    public final void setMouseMovedHandler(EventHandler<MouseEvent> value) {
        mouseMovedHandler.set(value);
    }
    public final ObjectProperty<EventHandler<MouseEvent>> mouseMovedHandlerProperty() {
        return mouseMovedHandler;
    }

    private ReadOnlyObjectWrapper<Bounds> hLabelLayoutBounds;
    public ReadOnlyObjectProperty<Bounds> hLabelLayoutBoundsProperty() {
        if (hLabelLayoutBounds == null) {
            hLabelLayoutBounds = new ReadOnlyObjectWrapper<>(this, "hLabelLayoutBounds");
            hLabelLayoutBounds.bind(hLabel.layoutBoundsProperty());
        }
        return hLabelLayoutBounds.getReadOnlyProperty();
    }
    public Bounds getHLabelLayoutBounds() {
        return hLabelLayoutBoundsProperty().get();
    }

    private ReadOnlyObjectWrapper<Bounds> vLabelLayoutBounds;
    public ReadOnlyObjectProperty<Bounds> vLabelLayoutBoundsProperty() {
        if (vLabelLayoutBounds == null) {
            vLabelLayoutBounds = new ReadOnlyObjectWrapper<>(this, "vLabelLayoutBounds");
            vLabelLayoutBounds.bind(vLabel.layoutBoundsProperty());
        }
        return vLabelLayoutBounds.getReadOnlyProperty();
    }
    public Bounds getVLabelLayoutBounds() {
        return vLabelLayoutBoundsProperty().get();
    }

    public Crosshair() {
        getStyleClass().setAll("crosshair-background");
        hLine.getStyleClass().setAll("crosshair-line", "horizontal");
        vLine.getStyleClass().setAll("crosshair-line", "vertical");
        info.getStyleClass().addAll("crosshair-info");
        hLabel.getStyleClass().addAll("crosshair-label", "horizontal");
        vLabel.getStyleClass().addAll("crosshair-label", "vertical");
        getChildren().addAll(hLine, vLine, infoPane, hLabel, vLabel);
        setPrefWidth(1);
        setPrefHeight(1);

        hLine.setStartX(0);
        hLine.endXProperty().bind(Bindings.createDoubleBinding(() -> isReactive() ? getWidth() : 0.,
                widthProperty(), reactive));
        DoubleBinding yBinding = Bindings.createDoubleBinding(() -> isReactive() ? getCrosshairY() : 0.,
                crosshairYProperty(), reactive);
        hLine.startYProperty().bind(yBinding);
        hLine.endYProperty().bind(yBinding);
        vLine.setStartY(0);
        vLine.endYProperty().bind(Bindings.createDoubleBinding(() -> isReactive() ? getHeight() : 0.,
                heightProperty(), reactive));
        DoubleBinding xBinding = Bindings.createDoubleBinding(() -> isReactive() ? getCrosshairX() : 0.,
                crosshairXProperty(), reactive);
        vLine.startXProperty().bind(xBinding);
        vLine.endXProperty().bind(xBinding);
        info.setWrapText(true);
        info.textProperty().bind(infoTextProperty());
        info.paddingProperty().bind(infoPaddingProperty());
        infoPane.setLayoutX(0);
        infoPane.setLayoutY(0);
        infoPane.prefWidthProperty().bind(widthProperty());
        infoPane.prefHeightProperty().bind(heightProperty());
        infoPane.alignmentProperty().bind(infoAlignmentProperty());
        hLabel.textProperty().bind(hLabelTextProperty());
        vLabel.textProperty().bind(vLabelTextProperty());
        hLabel.paddingProperty().bind(hLabelPaddingProperty());
        vLabel.paddingProperty().bind(vLabelPaddingProperty());
        hLabel.layoutXProperty().bind(Bindings.createDoubleBinding(() -> {
            if (!isReactive()) {
                return 0.;
            }
            if (getHLabelSide() == Side.RIGHT) {
                return getWidth() - getHLabelOffset() - hLabel.getBoundsInLocal().getWidth();
            } else {
                return getHLabelOffset();
            }
        }, hLabelOffsetProperty(), hLabelSideProperty(), widthProperty(), hLabel.boundsInLocalProperty(), reactive));
        hLabel.layoutYProperty().bind(Bindings.createDoubleBinding(() -> {
            return isReactive() ? getCrosshairY() - hLabel.getBoundsInLocal().getHeight()/2 : 0.;
        }, crosshairYProperty(), hLabel.boundsInLocalProperty(), reactive));
        vLabel.layoutXProperty().bind(Bindings.createDoubleBinding(() -> {
            return isReactive() ? getCrosshairX() - vLabel.getBoundsInLocal().getWidth()/2 : 0.;
        }, crosshairXProperty(), vLabel.boundsInLocalProperty(), reactive));
        vLabel.layoutYProperty().bind(Bindings.createDoubleBinding(() -> {
            if (!isReactive()) {
                return 0.;
            }
            if (getVLabelSide() == Side.TOP) {
                return getVLabelOffset();
            } else {
                return getHeight() - getVLabelOffset() - vLabel.getBoundsInLocal().getHeight();
            }
        }, vLabelOffsetProperty(), vLabelSideProperty(), heightProperty(), vLabel.boundsInLocalProperty(), reactive));

        addEventHandler(MouseEvent.MOUSE_EXITED, event -> deactivateDisplay());
        addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if (getLayoutBounds().contains(event.getX(), event.getY())) {
                activateDisplay(event);
            } else {
                deactivateDisplay();
            }
        });
        addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            if (getLayoutBounds().contains(event.getX(), event.getY())) {
                activateDisplay(event);
            } else {
                deactivateDisplay();
            }
        });
        deactivateDisplay();
    }

    private void activateDisplay(MouseEvent event) {
        // FIRST handle events
        EventHandler<MouseEvent> handler = getMouseMovedHandler();
        if (handler != null) {
            handler.handle(event);
        }
        reactive.set(true);
        hLine.setVisible(true);
        vLine.setVisible(true);
        if (info.textProperty().getValueSafe().isEmpty()) {
            info.setVisible(false);
        } else {
            info.setVisible(true);
        }
        if (hLabel.textProperty().getValueSafe().isEmpty()) {
            hLabel.setVisible(false);
        } else {
            hLabel.setVisible(true);
        }
        if (vLabel.textProperty().getValueSafe().isEmpty()) {
            vLabel.setVisible(false);
        } else {
            vLabel.setVisible(true);
        }
    }

    private void deactivateDisplay() {
        reactive.set(false);
        hLine.setVisible(false);
        vLine.setVisible(false);
        info.setVisible(false);
        hLabel.setVisible(false);
        vLabel.setVisible(false);
    }

}
