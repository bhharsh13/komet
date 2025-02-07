package dev.ikm.komet.kview.klfields;

import static dev.ikm.komet.kview.events.genediting.PropertyPanelEvent.OPEN_PANEL;
import static dev.ikm.komet.kview.events.genediting.PropertyPanelEvent.SHOW_EDIT_SEMANTIC_FIELDS;
import static dev.ikm.komet.kview.mvvm.model.DataModelHelper.obtainObservableField;
import static dev.ikm.komet.kview.mvvm.viewmodel.GenEditingViewModel.SEMANTIC;
import static dev.ikm.komet.kview.mvvm.viewmodel.GenEditingViewModel.WINDOW_TOPIC;

import dev.ikm.komet.framework.events.EvtBusFactory;
import dev.ikm.komet.framework.observable.ObservableField;
import dev.ikm.komet.framework.view.ViewProperties;
import dev.ikm.komet.kview.controls.KLReadOnlyDataTypeControl;
import dev.ikm.komet.kview.events.genediting.PropertyPanelEvent;
import dev.ikm.komet.kview.klfields.booleanfield.KlBooleanFieldFactory;
import dev.ikm.komet.kview.klfields.componentfield.KlComponentFieldFactory;
import dev.ikm.komet.kview.klfields.componentfield.KlComponentSetFieldFactory;
import dev.ikm.komet.kview.klfields.floatfield.KlFloatFieldFactory;
import dev.ikm.komet.kview.klfields.imagefield.KlImageFieldFactory;
import dev.ikm.komet.kview.klfields.integerfield.KlIntegerFieldFactory;
import dev.ikm.komet.kview.klfields.readonly.ReadOnlyKLFieldFactory;
import dev.ikm.komet.kview.klfields.stringfield.KlStringFieldFactory;
import dev.ikm.komet.kview.mvvm.model.DataModelHelper;
import dev.ikm.komet.kview.mvvm.viewmodel.GenEditingViewModel;
import dev.ikm.tinkar.common.id.PublicId;
import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.coordinate.stamp.calculator.Latest;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculator;
import dev.ikm.tinkar.entity.FieldRecord;
import dev.ikm.tinkar.entity.PatternEntityVersion;
import dev.ikm.tinkar.entity.SemanticEntityVersion;
import dev.ikm.tinkar.terms.EntityFacade;
import dev.ikm.tinkar.terms.TinkarTerm;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.layout.Pane;
import org.carlfx.cognitive.loader.InjectViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class KlFieldHelper {

    @InjectViewModel
    private static GenEditingViewModel genEditingViewModel;

    private static Separator createSeparator() {
        Separator separator = new Separator();
        separator.getStyleClass().add("field-separator");
        return separator;
    }
    public static void generateSemanticUIFields(ViewProperties viewProperties,
                                       Latest<SemanticEntityVersion> semanticEntityVersionLatest,
                                       Consumer<FieldRecord<Object>> updateUIConsumer) {
        semanticEntityVersionLatest.ifPresent(semanticEntityVersion -> {
            StampCalculator stampCalculator = viewProperties.calculator().stampCalculator();
            Latest<PatternEntityVersion> patternEntityVersionLatest = stampCalculator.latest(semanticEntityVersion.pattern());
            patternEntityVersionLatest.ifPresent(patternEntityVersion -> {
                List<FieldRecord<Object>> fieldRecords = DataModelHelper.fieldRecords(semanticEntityVersion, patternEntityVersion);
                fieldRecords.forEach(fieldRecord -> updateUIConsumer.accept(fieldRecord));
            });
        });
    }

    /**
     * Returns a list of observable fields and displays editable controls on a Pane using the latest semantic entity version.
     * @param viewProperties View Properties
     * @param container A JavaFX Pane. e.g. VBox
     * @param semanticEntityVersionLatest Semantic Entity Version object containing all field records and their field definitions & value
     * @return A list of observable fields
     */
    public static List<ObservableField<?>> displayEditableSemanticFields(ViewProperties viewProperties, Pane container, Latest<SemanticEntityVersion> semanticEntityVersionLatest) {
        ReadOnlyKLFieldFactory rowf = ReadOnlyKLFieldFactory.getInstance();
        List<ObservableField<?>> observableFields = new ArrayList<>();
        Consumer<FieldRecord<Object>> updateUIConsumer = (fieldRecord) -> {

            Node node = null;
            int dataTypeNid = fieldRecord.dataType().nid();
            ObservableField writeObservableField = obtainObservableField(viewProperties, semanticEntityVersionLatest, fieldRecord);
            ObservableField observableField = new ObservableField(writeObservableField.field(), true);
            observableFields.add(observableField);

            // TODO: this method below will be removed once the database has the capability to add and edit Image data types
            // TODO: then all the code will be inside an if clause just like for the other data types.
            maybeAddEditableImageControl(viewProperties, container, semanticEntityVersionLatest, observableField);

            if (dataTypeNid == TinkarTerm.COMPONENT_FIELD.nid()) {
                // load a read-only component
                KlComponentFieldFactory componentFieldFactory = new KlComponentFieldFactory();
                node = componentFieldFactory.create(observableField, viewProperties.nodeView(), true).klWidget();
            } else if (dataTypeNid == TinkarTerm.STRING_FIELD.nid() || fieldRecord.dataType().nid() == TinkarTerm.STRING.nid()) {
                KlStringFieldFactory stringFieldTextFactory = new KlStringFieldFactory();
                node = stringFieldTextFactory.create(observableField, viewProperties.nodeView(), true).klWidget();
            } else if (dataTypeNid == TinkarTerm.COMPONENT_ID_SET_FIELD.nid()) {
                KlComponentSetFieldFactory klComponentSetFieldFactory = new KlComponentSetFieldFactory();
                node = klComponentSetFieldFactory.create(observableField, viewProperties.nodeView(), true).klWidget();
            } else if (dataTypeNid == TinkarTerm.COMPONENT_ID_LIST_FIELD.nid()) {
                node = rowf.createReadOnlyComponentList(viewProperties, fieldRecord);
            } else if (dataTypeNid == TinkarTerm.DITREE_FIELD.nid()) {
                node = rowf.createReadOnlyDiTree(viewProperties, fieldRecord);
            } else if (dataTypeNid == TinkarTerm.FLOAT_FIELD.nid() || fieldRecord.dataType().nid() == TinkarTerm.FLOAT.nid()) {
                KlFloatFieldFactory klFloatFieldFactory = new KlFloatFieldFactory();
                node = klFloatFieldFactory.create(observableField, viewProperties.nodeView(), true).klWidget();
            } else if (dataTypeNid == TinkarTerm.INTEGER_FIELD.nid() || fieldRecord.dataType().nid() == TinkarTerm.INTEGER_FIELD.nid()) {
                KlIntegerFieldFactory klIntegerFieldFactory = new KlIntegerFieldFactory();
                node = klIntegerFieldFactory.create(observableField, viewProperties.nodeView(), true).klWidget();
            } else if (dataTypeNid == TinkarTerm.BOOLEAN_FIELD.nid()) {
                KlBooleanFieldFactory klBooleanFieldFactory = new KlBooleanFieldFactory();
                node = klBooleanFieldFactory.create(observableField, viewProperties.nodeView(), true).klWidget();
            }
//            else if (dataTypeNid == TinkarTerm.IMAGE_FIELD.nid()) {
//                node = rowf.createReadOnlyComponentSet(viewProperties, fieldRecord);
//                ObservableField<> observableField = obtainObservableField(viewProperties, semanticEntityVersionLatest, fieldRecord);
//                observableFields.add(observableField);
//            }
            // Add to VBox
            if (node != null) {
                container.getChildren().add(node);
                // Add separator
                container.getChildren().add(createSeparator());
            }
        };
        generateSemanticUIFields(viewProperties, semanticEntityVersionLatest, updateUIConsumer);

        hasAddedEditableImage = false;

        return observableFields;
    }

    public static List<ObservableField<?>> displayReadOnlySemanticFields(ViewProperties viewProperties, Pane container, Latest<SemanticEntityVersion> semanticEntityVersionLatest) {

            //FIXME use a different factory
            ReadOnlyKLFieldFactory rowf = ReadOnlyKLFieldFactory.getInstance();
            List<ObservableField<?>> observableFields = new ArrayList<>();
            Consumer<FieldRecord<Object>> updateUIConsumer = (fieldRecord) -> {

                Node readOnlyNode = null;
                int dataTypeNid = fieldRecord.dataType().nid();
                ObservableField<?> writeObservableField = obtainObservableField(viewProperties, semanticEntityVersionLatest, fieldRecord);
                ObservableField observableField = new ObservableField(writeObservableField.field(), false);
                observableFields.add(observableField);

                // TODO: this method below will be removed once the database has the capability to add and edit Image data types
                // TODO: then all the code will be inside an if clause just like for the other data types.
                maybeAddReadOnlyImageControl(viewProperties, container, semanticEntityVersionLatest, observableField);

                // substitute each data type.
                if (dataTypeNid == TinkarTerm.COMPONENT_FIELD.nid()) {
                    // load a read-only component
                    KlComponentFieldFactory klComponentFieldFactory = new KlComponentFieldFactory();
                    readOnlyNode = klComponentFieldFactory.create(observableField, viewProperties.nodeView(), false).klWidget();
                } else if (dataTypeNid == TinkarTerm.STRING_FIELD.nid() || fieldRecord.dataType().nid() == TinkarTerm.STRING.nid()) {
                    KlStringFieldFactory klStringFieldFactory = new KlStringFieldFactory();
                    readOnlyNode = klStringFieldFactory.create(observableField, viewProperties.nodeView(), false).klWidget();
                } else if (dataTypeNid == TinkarTerm.COMPONENT_ID_SET_FIELD.nid()) {
                    KlComponentSetFieldFactory klComponentSetFieldFactory = new KlComponentSetFieldFactory();
                    readOnlyNode = klComponentSetFieldFactory.create(observableField, viewProperties.nodeView(), false).klWidget();
                } else if (dataTypeNid == TinkarTerm.COMPONENT_ID_LIST_FIELD.nid()) {
                    readOnlyNode = rowf.createReadOnlyComponentList(viewProperties, fieldRecord);
                } else if (dataTypeNid == TinkarTerm.DITREE_FIELD.nid()) {
                    readOnlyNode = rowf.createReadOnlyDiTree(viewProperties, fieldRecord);
                } else if (dataTypeNid == TinkarTerm.FLOAT_FIELD.nid()) {
                    KlFloatFieldFactory klFloatFieldFactory = new KlFloatFieldFactory();
                    readOnlyNode = klFloatFieldFactory.create(observableField, viewProperties.nodeView(), false).klWidget();
                } else if (dataTypeNid == TinkarTerm.INTEGER_FIELD.nid()) {
                    KlIntegerFieldFactory klIntegerFieldFactory = new KlIntegerFieldFactory();
                    KLReadOnlyDataTypeControl<Integer> readOnlyNode2 = klIntegerFieldFactory.create(observableField, viewProperties.nodeView(), false).klWidget();
                    readOnlyNode2.setOnEditAction(()-> {
                        System.out.println("entered setOnEditAction()");
//                        EntityFacade semantic = genEditingViewModel.getPropertyValue(SEMANTIC);
//                        // notify bump out to display edit fields in bump out area.
//                        EvtBusFactory.getDefaultEvtBus()
//                                .publish(genEditingViewModel.getPropertyValue(WINDOW_TOPIC),
//                                        new PropertyPanelEvent(actionEvent.getSource(),
//                                                SHOW_EDIT_SEMANTIC_FIELDS, semantic));
//                        // open properties bump out.
//                        EvtBusFactory.getDefaultEvtBus().publish(genEditingViewModel.getPropertyValue(WINDOW_TOPIC), new PropertyPanelEvent(actionEvent.getSource(), OPEN_PANEL));
                    });
                    readOnlyNode = readOnlyNode2;
                }  else if (dataTypeNid == TinkarTerm.BOOLEAN_FIELD.nid()) {
                    KlBooleanFieldFactory klBooleanFieldFactory = new KlBooleanFieldFactory();
                    readOnlyNode = klBooleanFieldFactory.create(observableField, viewProperties.nodeView(), false).klWidget();
                }
                // Add to VBox
                if (readOnlyNode != null) {
                    container.getChildren().add(readOnlyNode);
                }
            };
            generateSemanticUIFields(viewProperties, semanticEntityVersionLatest, updateUIConsumer);

            hasAddedReadOnlyImage = false;

            return observableFields;
        }


    // TODO: These methods below are in temporarily so we can add a Image data type that doesn't fetch anything from the database.
    // TODO: once the database has the capability for Image Data types we can remove these methods
    private static boolean hasAddedReadOnlyImage = false;
    private static boolean hasAddedEditableImage = false;

    private static void maybeAddEditableImageControl(ViewProperties viewProperties, Pane container, Latest<SemanticEntityVersion> semanticEntityVersionLatest, ObservableField observableField) {
        if (PublicId.equals(semanticEntityVersionLatest.get().entity().publicId(), PublicIds.of(UUID.fromString("48633874-f3d2-434a-9f11-2a07e4c4311b")))
                && !hasAddedEditableImage) {
            KlImageFieldFactory imageFieldFactory = new KlImageFieldFactory();
            Node node = imageFieldFactory.create(observableField, viewProperties.nodeView(), true).klWidget();
            if (node != null) {
                container.getChildren().add(node);
                // Add separator
                container.getChildren().add(createSeparator());
                hasAddedEditableImage = true;
            }
        }
    }

    private static void maybeAddReadOnlyImageControl(ViewProperties viewProperties, Pane container, Latest<SemanticEntityVersion> semanticEntityVersionLatest, ObservableField observableField) {
        if (PublicId.equals(semanticEntityVersionLatest.get().entity().publicId(), PublicIds.of(UUID.fromString("48633874-f3d2-434a-9f11-2a07e4c4311b")))
                && !hasAddedReadOnlyImage) {
            KlImageFieldFactory imageFieldFactory = new KlImageFieldFactory();
            Node readOnlyNode = imageFieldFactory.create(observableField, viewProperties.nodeView(), false).klWidget();
            if (readOnlyNode != null) {
                container.getChildren().add(readOnlyNode);
                hasAddedReadOnlyImage = true;
            }
        }
    }
}
