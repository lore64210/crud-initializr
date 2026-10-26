import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import {
  Background,
  BackgroundVariant,
  BaseEdge,
  Controls,
  EdgeLabelRenderer,
  Handle,
  MarkerType,
  MiniMap,
  Position,
  ReactFlow,
  ReactFlowProvider,
  getSmoothStepPath,
  useEdgesState,
  useNodesState,
  useReactFlow,
} from "@xyflow/react";
import { Graph, layout } from "@dagrejs/dagre";
import {
  Alert,
  Button,
  Collapse,
  IconButton,
  Snackbar,
  Tooltip,
  Typography,
} from "@mui/material";
import AccountTreeIcon from "@mui/icons-material/AccountTree";
import AddIcon from "@mui/icons-material/Add";
import CenterFocusStrongIcon from "@mui/icons-material/CenterFocusStrong";
import CloseIcon from "@mui/icons-material/Close";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import FullscreenIcon from "@mui/icons-material/Fullscreen";
import FullscreenExitIcon from "@mui/icons-material/FullscreenExit";
import RestartAltIcon from "@mui/icons-material/RestartAlt";
import "@xyflow/react/dist/style.css";
import RelationDialog from "./RelationDialog";
import { formatStringToCamelCase } from "../service/service";
import "../css/app.css";

const NODE_WIDTH = 250;
const NODE_HEIGHT = 152;
const MISSING_WIDTH = 190;
const MISSING_HEIGHT = 76;
const MAX_VISIBLE_ATTRIBUTES = 3;

/** Greens that stay distinguishable on the dark canvas. */
const RELATION_COLORS = [
  "#80ea6e",
  "#b6f26b",
  "#4fd18b",
  "#8fdc60",
  "#61c9a3",
  "#c3f47a",
];

const SQL_RELATION_TYPES = [
  "CUSTOM",
  "CUSTOM_LIST",
  "CUSTOM_ONE_TO_ONE",
  "CUSTOM_MANY_TO_MANY",
];
const MONGO_COMPOSITION_TYPES = ["COMPOSITION", "COMPOSITION_LIST"];

const isRelationType = (type) => SQL_RELATION_TYPES.includes(type);
const isCompositionType = (type) => MONGO_COMPOSITION_TYPES.includes(type);
const isLinkType = (type) => isRelationType(type) || isCompositionType(type);
const isListType = (type) =>
  type === "CUSTOM_LIST" ||
  type === "CUSTOM_MANY_TO_MANY" ||
  type === "COMPOSITION_LIST";

const TYPE_LABELS = {
  STRING: "String",
  INTEGER: "Integer",
  LONG: "Long",
  BOOLEAN: "Boolean",
  CUSTOM: "N:1",
  CUSTOM_LIST: "1:N",
  CUSTOM_ONE_TO_ONE: "1:1",
  CUSTOM_MANY_TO_MANY: "N:M",
  COMPOSITION: "Composición 1:1",
  COMPOSITION_LIST: "Composición 1:N",
};

function normalizeName(value) {
  return (value || "").trim().toLowerCase();
}

function pluralize(value) {
  if (!value) return "relaciones";
  if (value.endsWith("s")) return value;
  if (value.endsWith("z")) return `${value.slice(0, -1)}ces`;
  return `${value}s`;
}

function suggestRelationName(source, targetName, type) {
  const base = formatStringToCamelCase(targetName) || "relacion";
  const suggestion = isListType(type) ? pluralize(base) : base;
  const used = new Set(
    (source?.attributes || []).map((attribute) =>
      formatStringToCamelCase(attribute.name).toLowerCase(),
    ),
  );

  let candidate = suggestion;
  let index = 2;
  while (used.has(formatStringToCamelCase(candidate).toLowerCase())) {
    candidate = `${suggestion}${index}`;
    index += 1;
  }
  return candidate;
}

function colorForEntity(uuid) {
  let hash = 0;
  for (let index = 0; index < uuid.length; index++) {
    hash = (hash * 31 + uuid.charCodeAt(index)) >>> 0;
  }
  return RELATION_COLORS[hash % RELATION_COLORS.length];
}

function attributeSummary(attribute) {
  const link = isLinkType(attribute.type);
  const name = attribute.name.trim();
  if (link) {
    const target = (attribute.customType || "").trim();
    return {
      name: name || "sin nombre",
      typeLabel: target
        ? `→ ${target}${isListType(attribute.type) ? " []" : ""}`
        : "sin destino",
      relation: true,
      composition: isCompositionType(attribute.type),
      incomplete: !target,
    };
  }
  return {
    name: name || "sin nombre",
    typeLabel: TYPE_LABELS[attribute.type] || attribute.type,
    relation: false,
    composition: false,
    incomplete: false,
  };
}

function buildEntityGraph(entities, database = "POSTGRESQL") {
  const isMongo = database === "MONGODB";
  const byName = new Map();
  for (const entity of entities) {
    const key = normalizeName(entity.entityName);
    if (key) byName.set(key, entity);
  }

  const missingNodes = new Map();
  const edges = [];

  const nodes = entities.map((entity) => {
    const attributes = (entity.attributes || []).map(attributeSummary);
    const ordered = [
      ...attributes.filter((attribute) => attribute.relation),
      ...attributes.filter((attribute) => !attribute.relation),
    ];
    const relationCount = attributes.filter(
      (attribute) => attribute.relation && !attribute.composition,
    ).length;
    const compositionCount = attributes.filter(
      (attribute) => attribute.relation && attribute.composition,
    ).length;
    const incompleteCount = attributes.filter(
      (attribute) => attribute.incomplete,
    ).length;
    const linkCount = relationCount + compositionCount;

    let linkKind = isMongo ? "composición" : "relación";
    if (compositionCount > 0 && relationCount === 0) {
      linkKind = "composición";
    } else if (compositionCount > 0 && relationCount > 0) {
      linkKind = "vínculo";
    }

    return {
      id: entity.uuid,
      type: "entity",
      position: { x: 0, y: 0 },
      data: {
        title: entity.entityName.trim() || "Entidad sin nombre",
        unnamed: !entity.entityName.trim(),
        embedded: !!entity.embedded,
        attributes: ordered.slice(0, MAX_VISIBLE_ATTRIBUTES),
        hiddenAttributeCount: Math.max(
          0,
          ordered.length - MAX_VISIBLE_ATTRIBUTES,
        ),
        totalAttributes: attributes.length,
        relationCount,
        compositionCount,
        linkCount,
        linkKind,
        incompleteCount,
        connectable: !entity.embedded,
      },
    };
  });

  for (const entity of entities) {
    const color = colorForEntity(entity.uuid);
    for (const attribute of entity.attributes || []) {
      if (!isLinkType(attribute.type)) continue;

      const targetName = (attribute.customType || "").trim();
      if (!targetName) continue;

      const target = byName.get(normalizeName(targetName));
      const list = isListType(attribute.type);
      const composition = isCompositionType(attribute.type);
      const edgeColor = target ? color : "#ff8a75";
      const cardinality =
        attribute.type === "CUSTOM"
          ? "N:1"
          : attribute.type === "CUSTOM_MANY_TO_MANY"
            ? "N:M"
            : attribute.type === "CUSTOM_LIST" ||
                attribute.type === "COMPOSITION_LIST"
              ? "1:N"
              : "1:1";

      const data = {
        label: attribute.name.trim() || "sin nombre",
        cardinality,
        list,
        composition,
        missing: !target,
        color: edgeColor,
      };

      if (!target) {
        const missingId = `missing:${normalizeName(targetName)}`;
        if (!missingNodes.has(missingId)) {
          missingNodes.set(missingId, {
            id: missingId,
            type: "missing",
            position: { x: 0, y: 0 },
            data: { label: targetName },
            draggable: true,
            selectable: false,
            connectable: false,
          });
        }
      }

      edges.push({
        id: attribute.uuid,
        source: entity.uuid,
        target: target ? target.uuid : `missing:${normalizeName(targetName)}`,
        type: "entityRelation",
        zIndex: 5,
        reconnectable: "target",
        markerEnd: {
          type: MarkerType.ArrowClosed,
          color: edgeColor,
          width: 18,
          height: 18,
        },
        data,
      });
    }
  }

  return { nodes: [...nodes, ...missingNodes.values()], edges };
}

function layoutGraph(nodes, edges) {
  const graph = new Graph({ multigraph: true });
  graph.setGraph({
    rankdir: "TB",
    nodesep: 70,
    ranksep: 95,
    edgesep: 40,
    marginx: 40,
    marginy: 40,
  });
  graph.setDefaultEdgeLabel(() => ({}));

  for (const node of nodes) {
    graph.setNode(node.id, {
      width: node.type === "missing" ? MISSING_WIDTH : NODE_WIDTH,
      height: node.type === "missing" ? MISSING_HEIGHT : NODE_HEIGHT,
    });
  }

  for (const edge of edges) {
    if (edge.source === edge.target) continue;
    graph.setEdge(edge.source, edge.target, { weight: 1, minlen: 1 }, edge.id);
  }

  layout(graph);

  const positions = {};
  for (const node of nodes) {
    const placed = graph.node(node.id);
    if (!placed) continue;
    const width = placed.width ?? NODE_WIDTH;
    const height = placed.height ?? NODE_HEIGHT;
    positions[node.id] = {
      x: (placed.x ?? 0) - width / 2,
      y: (placed.y ?? 0) - height / 2,
    };
  }
  return positions;
}

function EntityNode({ data, selected }) {
  const linkKind =
    data.linkKind === "composición"
      ? data.linkCount === 1
        ? "composición"
        : "composiciones"
      : data.linkKind === "vínculo"
        ? data.linkCount === 1
          ? "vínculo"
          : "vínculos"
        : data.linkCount === 1
          ? "relación"
          : "relaciones";

  return (
    <div
      className={`relation-node${selected ? " relation-node--selected" : ""}${
        data.unnamed ? " relation-node--unnamed" : ""
      }${data.embedded ? " relation-node--embedded" : ""}`}
      title={`${data.title} · click para editar`}
    >
      <Handle
        type="target"
        position={Position.Top}
        isConnectable
        className="relation-node__handle relation-node__handle--target"
        title="Soltá un vínculo acá"
      />
      <div className="relation-node__header">
        <span className="relation-node__name">{data.title}</span>
        <span className="relation-node__badges">
          {data.embedded && (
            <span className="relation-node__badge relation-node__badge--embedded">
              EMBED
            </span>
          )}
          <span className="relation-node__badge">{data.totalAttributes}</span>
        </span>
      </div>
      <div className="relation-node__body">
        {data.attributes.length === 0 && (
          <span className="relation-node__empty">Sin atributos</span>
        )}
        {data.attributes.map((attribute, index) => (
          <div className="relation-node__row" key={index}>
            <span className="relation-node__row-name">{attribute.name}</span>
            <span
              className={`relation-node__row-type${
                attribute.relation ? " relation-node__row-type--relation" : ""
              }`}
            >
              {attribute.typeLabel}
            </span>
          </div>
        ))}
        {data.hiddenAttributeCount > 0 && (
          <span className="relation-node__more">
            +{data.hiddenAttributeCount} atributo
            {data.hiddenAttributeCount === 1 ? "" : "s"} más
          </span>
        )}
      </div>
      <div className="relation-node__footer">
        <span>
          {data.linkCount} {linkKind}
        </span>
        {data.incompleteCount > 0 && (
          <span className="relation-node__warning">
            {data.incompleteCount} sin destino
          </span>
        )}
      </div>
      <Handle
        type="source"
        position={Position.Bottom}
        isConnectable={data.connectable !== false}
        className="relation-node__handle relation-node__handle--source"
        title={
          data.connectable === false
            ? "Los documentos embebidos no pueden originar composiciones"
            : "Arrastrá para crear un vínculo"
        }
      >
        {data.connectable !== false && (
          <span className="relation-node__handle-plus">
            <AddIcon sx={{ fontSize: "9px", display: "block" }} />
          </span>
        )}
      </Handle>
    </div>
  );
}

function MissingEntityNode({ data }) {
  return (
    <div className="relation-node relation-node--missing">
      <Handle
        type="target"
        position={Position.Top}
        isConnectable={false}
        className="relation-node__handle"
      />
      <span className="relation-node__missing-label">SIN ENTIDAD</span>
      <span className="relation-node__missing-name">{data.label}</span>
    </div>
  );
}

function EntityRelationEdge({
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  markerEnd,
  data,
  selected,
}) {
  const [path, labelX, labelY] = getSmoothStepPath({
    sourceX,
    sourceY,
    sourcePosition,
    targetX,
    targetY,
    targetPosition,
    borderRadius: 16,
  });

  return (
    <>
      <BaseEdge
        id={id}
        path={path}
        markerEnd={markerEnd}
        style={{
          stroke: data.color,
          strokeWidth: selected ? 2.8 : 1.9,
          strokeDasharray: data.composition
            ? "3 4"
            : data.list
              ? "7 5"
              : undefined,
          opacity: data.missing ? 0.9 : 0.95,
        }}
      />
      <EdgeLabelRenderer>
        <div
          className={`relation-edge-label${data.list ? " relation-edge-label--list" : ""}${
            data.composition ? " relation-edge-label--composition" : ""
          }${selected ? " relation-edge-label--active" : ""}`}
          style={{
            transform: `translate(-50%, -50%) translate(${labelX}px, ${labelY}px)`,
            borderColor: data.color,
            color: data.color,
          }}
        >
          <span className="relation-edge-label__name">{data.label}</span>
          <span className="relation-edge-label__kind">
            {data.composition ? `compone · ${data.cardinality}` : data.cardinality}
          </span>
        </div>
      </EdgeLabelRenderer>
    </>
  );
}

const nodeTypes = { entity: EntityNode, missing: MissingEntityNode };
const edgeTypes = { entityRelation: EntityRelationEdge };

function minimapNodeColor(node) {
  if (node.type === "missing") return "#ff8a75";
  return "#6db33f";
}

function GraphCanvas({
  nodes,
  edges,
  onNodesChange,
  onEdgesChange,
  onNodeDragStop,
  onNodeClick,
  onConnect,
  onConnectEnd,
  onEdgeClick,
  onReconnect,
  fullscreen,
}) {
  return (
    <div
      className={`relation-graph-canvas${
        fullscreen ? " relation-graph-canvas--fullscreen" : ""
      }`}
    >
      {nodes.length === 0 ? (
        <div className="relation-graph-empty">
          Todavía no hay entidades para mostrar.
        </div>
      ) : (
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onNodeDragStop={onNodeDragStop}
          onNodeClick={onNodeClick}
          onConnect={onConnect}
          onConnectEnd={onConnectEnd}
          onEdgeClick={onEdgeClick}
          onReconnect={onReconnect}
          nodeTypes={nodeTypes}
          edgeTypes={edgeTypes}
          fitView
          fitViewOptions={{ padding: 0.22 }}
          minZoom={0.25}
          maxZoom={1.6}
          nodesConnectable
          connectOnClick={false}
          connectionRadius={50}
          reconnectRadius={14}
          connectionLineStyle={{
            stroke: "#80ea6e",
            strokeWidth: 2,
            strokeDasharray: "6 4",
          }}
          edgesReconnectable
          elementsSelectable
          panOnScroll={false}
          zoomOnScroll={false}
          preventScrolling={false}
          proOptions={{ hideAttribution: true }}
          defaultEdgeOptions={{ zIndex: 5 }}
        >
          <Background
            variant={BackgroundVariant.Dots}
            gap={20}
            size={1}
            color="rgba(128, 234, 110, 0.2)"
          />
          <Controls showInteractive={false} />
          <MiniMap
            pannable
            zoomable
            nodeColor={minimapNodeColor}
            maskColor="rgba(12, 17, 14, 0.72)"
            style={{
              background: "#141b16",
              border: "1px solid rgba(109, 179, 63, 0.4)",
              borderRadius: 6,
              width: 160,
              height: 108,
            }}
          />
        </ReactFlow>
      )}
    </div>
  );
}

function RelationGraphShell({
  entities,
  database = "POSTGRESQL",
  selectedEntityUuid,
  onSelectEntity,
  onAddEntity,
  onCreateRelation,
  onUpdateRelation,
  onDeleteRelation,
}) {
  const { fitView } = useReactFlow();
  const [fullscreen, setFullscreen] = useState(false);
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [dialog, setDialog] = useState(null);
  const [notice, setNotice] = useState(null);
  const manualPositions = useRef({});
  const structureRef = useRef("");
  const onSelectRef = useRef(onSelectEntity);
  const connectionCompletedRef = useRef(false);
  onSelectRef.current = onSelectEntity;

  const sourcesById = useMemo(
    () => new Map(entities.map((entity) => [entity.uuid, entity])),
    [entities],
  );

  const isMongo = database === "MONGODB";
  const graph = useMemo(
    () => buildEntityGraph(entities, database),
    [entities, database],
  );
  const structureKey = useMemo(() => {
    const nodeIds = graph.nodes
      .map((node) => node.id)
      .sort()
      .join("|");
    const edgeKeys = graph.edges
      .map((edge) => `${edge.source}>${edge.target}`)
      .sort()
      .join("|");
    return `${nodeIds}::${edgeKeys}`;
  }, [graph]);

  useEffect(() => {
    const structureChanged = structureRef.current !== structureKey;
    structureRef.current = structureKey;
    const autoPositions = structureChanged
      ? layoutGraph(graph.nodes, graph.edges)
      : null;

    setNodes((current) => {
      const currentById = new Map(current.map((node) => [node.id, node]));
      const next = graph.nodes.map((incoming) => {
        const existing = currentById.get(incoming.id);
        const nextData = incoming.data;

        if (!existing) {
          return {
            ...incoming,
            position:
              manualPositions.current[incoming.id] ??
              autoPositions?.[incoming.id] ?? { x: 0, y: 0 },
            data: nextData,
          };
        }

        const position =
          manualPositions.current[incoming.id] ??
          autoPositions?.[incoming.id] ??
          existing.position;
        const dataChanged =
          JSON.stringify(existing.data) !== JSON.stringify(nextData);

        if (!dataChanged && !autoPositions?.[incoming.id]) {
          return existing;
        }
        return { ...existing, position, data: nextData };
      });

      const unchanged =
        next.length === current.length &&
        next.every((node, index) => node === current[index]);
      return unchanged ? current : next;
    });

    setEdges((current) => {
      if (current.length !== graph.edges.length) return graph.edges;
      const currentById = new Map(current.map((edge) => [edge.id, edge]));
      let changed = false;
      const next = graph.edges.map((incoming) => {
        const existing = currentById.get(incoming.id);
        if (
          existing &&
          existing.source === incoming.source &&
          existing.target === incoming.target &&
          JSON.stringify(existing.data) === JSON.stringify(incoming.data)
        ) {
          return existing;
        }
        changed = true;
        return incoming;
      });
      return changed ? next : current;
    });
  }, [graph, structureKey, setNodes, setEdges]);

  useEffect(() => {
    setNodes((current) => {
      let changed = false;
      const next = current.map((node) => {
        const selected = node.id === selectedEntityUuid;
        if (!!node.selected === selected) return node;
        changed = true;
        return { ...node, selected };
      });
      return changed ? next : current;
    });
  }, [selectedEntityUuid, setNodes]);

  useEffect(() => {
    if (graph.nodes.length === 0) return undefined;
    const timer = window.setTimeout(() => {
      fitView({ padding: 0.22, duration: 250 });
    }, 80);
    return () => window.clearTimeout(timer);
  }, [structureKey, fullscreen, fitView, graph.nodes.length]);

  useEffect(() => {
    if (!fullscreen) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const handleKeyDown = (event) => {
      if (dialog || event.key !== "Escape") return;
      const tag = event.target?.tagName;
      if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return;
      setFullscreen(false);
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [fullscreen, dialog]);

  const handleNodeDragStop = useCallback((_event, node) => {
    manualPositions.current[node.id] = { ...node.position };
  }, []);

  const markConnectionCompleted = useCallback(() => {
    connectionCompletedRef.current = true;
    window.setTimeout(() => {
      connectionCompletedRef.current = false;
    }, 250);
  }, []);

  const handleNodeClick = useCallback(
    (_event, node) => {
      if (connectionCompletedRef.current) return;
      if (node.type !== "entity") return;
      if (fullscreen) setFullscreen(false);
      onSelectRef.current?.(node.id);
    },
    [fullscreen],
  );

  const openCreateRelation = useCallback(
    (sourceId, targetId) => {
      if (!sourceId || !targetId) return;
      if (sourceId === targetId) {
        setNotice({
          severity: "warning",
          message: "Una entidad no puede relacionarse consigo misma.",
        });
        return;
      }

      const source = sourcesById.get(sourceId);
      const target = sourcesById.get(targetId);
      if (!source || !target) return;

      if (source.embedded) {
        setNotice({
          severity: "warning",
          message:
            "Un documento embebido no puede originar composiciones. Marcá como raíz la entidad origen.",
        });
        return;
      }

      if (!target.entityName.trim()) {
        setNotice({
          severity: "warning",
          message: "La entidad destino necesita un nombre antes de vincularla.",
        });
        return;
      }

      if (isMongo && !target.embedded) {
        setNotice({
          severity: "warning",
          message: `Marcá "${target.entityName.trim()}" como documento embebido para usarla en una composición.`,
        });
        return;
      }

      const initialType = isMongo ? "COMPOSITION" : "CUSTOM";
      setDialog({
        mode: "create",
        sourceId,
        targetId,
        sourceName: source.entityName.trim() || "Entidad sin nombre",
        targetName: target.entityName.trim(),
        targetExists: true,
        targetEmbedded: !!target.embedded,
        initialName: suggestRelationName(
          source,
          target.entityName,
          initialType,
        ),
        initialType,
        otherNames: source.attributes.map((attribute) => attribute.name),
      });
    },
    [isMongo, sourcesById],
  );

  const handleConnect = useCallback(
    (connection) => {
      if (!connection?.source || !connection?.target) return;
      markConnectionCompleted();
      openCreateRelation(connection.source, connection.target);
    },
    [openCreateRelation, markConnectionCompleted],
  );

  const handleConnectEnd = useCallback(
    (event, state) => {
      if (state?.isValid) return;
      const sourceId = state?.fromNode?.id;
      if (!sourceId) return;

      const clientPoint =
        "changedTouches" in event ? event.changedTouches?.[0] : event;
      if (!clientPoint) return;

      const nodeElement = document
        .elementsFromPoint(clientPoint.clientX, clientPoint.clientY)
        .find((element) => element.classList?.contains("react-flow__node"));
      if (!nodeElement) return;

      markConnectionCompleted();

      if (nodeElement.classList.contains("react-flow__node-missing")) {
        setNotice({
          severity: "warning",
          message: "La entidad destino no existe.",
        });
        return;
      }

      const targetId = nodeElement.getAttribute("data-id");
      if (targetId) openCreateRelation(sourceId, targetId);
    },
    [openCreateRelation, markConnectionCompleted],
  );

  const handleEdgeClick = useCallback(
    (_event, edge) => {
      const source = sourcesById.get(edge.source);
      const attribute = source?.attributes.find(
        (item) => item.uuid === edge.id,
      );
      if (!source || !attribute) return;

      const target = sourcesById.get(edge.target);
      setDialog({
        mode: "edit",
        sourceId: source.uuid,
        targetId: edge.target,
        attributeId: attribute.uuid,
        sourceName: source.entityName.trim() || "Entidad sin nombre",
        targetName: attribute.customType.trim() || "Sin entidad",
        targetExists: !!target,
        targetEmbedded: !!target?.embedded,
        initialName: attribute.name,
        initialType: attribute.type,
        otherNames: source.attributes
          .filter((item) => item.uuid !== attribute.uuid)
          .map((item) => item.name),
      });
    },
    [sourcesById],
  );

  const handleReconnect = useCallback(
    (oldEdge, connection) => {
      if (!oldEdge?.source || !connection?.target) return;
      markConnectionCompleted();

      if (connection.target === oldEdge.source) {
        setNotice({
          severity: "warning",
          message: "Una entidad no puede vincularse consigo misma.",
        });
        return;
      }

      if (connection.target.startsWith("missing:")) {
        setNotice({
          severity: "warning",
          message: "La entidad destino no existe.",
        });
        return;
      }

      const source = sourcesById.get(oldEdge.source);
      const target = sourcesById.get(connection.target);
      const attribute = source?.attributes.find(
        (item) => item.uuid === oldEdge.id,
      );
      if (!source || !target || !attribute) return;

      if (source.embedded) {
        setNotice({
          severity: "warning",
          message: "Un documento embebido no puede originar composiciones.",
        });
        return;
      }

      if (isMongo && !target.embedded) {
        setNotice({
          severity: "warning",
          message: `Marcá "${target.entityName.trim()}" como documento embebido para mover la composición.`,
        });
        return;
      }

      if (!isMongo && target.embedded) {
        setNotice({
          severity: "warning",
          message: `La entidad "${target.entityName.trim()}" está marcada como embebida y no es válida para SQL.`,
        });
        return;
      }

      const targetName = target.entityName.trim();
      if (!targetName) {
        setNotice({
          severity: "warning",
          message: "La entidad destino necesita un nombre antes de vincularla.",
        });
        return;
      }
      if ((attribute.customType || "").trim() === targetName) return;

      onUpdateRelation?.(oldEdge.source, oldEdge.id, {
        customType: targetName,
      });
      const linkWord = isCompositionType(attribute.type)
        ? "Composición"
        : "Relación";
      setNotice({
        severity: "success",
        message: `${linkWord} movida: ${
          source.entityName.trim() || "Entidad sin nombre"
        } → ${targetName}. Click en la arista para renombrarla.`,
      });
    },
    [isMongo, markConnectionCompleted, onUpdateRelation, sourcesById],
  );

  const handleSaveRelation = useCallback(
    ({ name, type }) => {
      if (!dialog) return;
      const linkWord = isCompositionType(type) ? "Composición" : "Relación";
      if (dialog.mode === "create") {
        const target = sourcesById.get(dialog.targetId);
        const targetName = target?.entityName.trim() || dialog.targetName;
        onCreateRelation?.(dialog.sourceId, targetName, { name, type });
        setNotice({
          severity: "success",
          message: `${linkWord} creada: ${dialog.sourceName} → ${targetName}`,
        });
      } else {
        onUpdateRelation?.(dialog.sourceId, dialog.attributeId, {
          name,
          type,
        });
        setNotice({ severity: "success", message: `${linkWord} actualizada` });
      }
      setDialog(null);
    },
    [dialog, onCreateRelation, onUpdateRelation, sourcesById],
  );

  const handleDeleteRelation = useCallback(() => {
    if (!dialog || dialog.mode !== "edit") return;
    onDeleteRelation?.(dialog.sourceId, dialog.attributeId);
    setNotice({ severity: "info", message: "Vínculo eliminado" });
    setDialog(null);
  }, [dialog, onDeleteRelation]);

  const closeDialog = useCallback(() => setDialog(null), []);

  const handleAddEntity = useCallback(() => {
    if (fullscreen) setFullscreen(false);
    onAddEntity?.();
  }, [fullscreen, onAddEntity]);

  const relayout = useCallback(() => {
    manualPositions.current = {};
    const positions = layoutGraph(graph.nodes, graph.edges);
    setNodes((current) =>
      current.map((node) => ({
        ...node,
        position: positions[node.id] ?? node.position,
      })),
    );
    window.setTimeout(() => fitView({ padding: 0.22, duration: 300 }), 40);
  }, [fitView, graph.edges, graph.nodes, setNodes]);

  const canvas = (
    <GraphCanvas
      nodes={nodes}
      edges={edges}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      onNodeDragStop={handleNodeDragStop}
      onNodeClick={handleNodeClick}
      onConnect={handleConnect}
      onConnectEnd={handleConnectEnd}
      onEdgeClick={handleEdgeClick}
      onReconnect={handleReconnect}
      fullscreen={fullscreen}
    />
  );

  const actions = (
    <>
      <Button
        color="success"
        size="small"
        variant="contained"
        startIcon={<AddIcon />}
        onClick={handleAddEntity}
      >
        Agregar entidad
      </Button>
      <Button
        color="success"
        size="small"
        startIcon={<RestartAltIcon />}
        onClick={relayout}
      >
        Reordenar
      </Button>
      <Button
        color="success"
        size="small"
        startIcon={<CenterFocusStrongIcon />}
        onClick={() => fitView({ padding: 0.22, duration: 300 })}
      >
        Centrar
      </Button>
      {fullscreen ? (
        <Button
          color="success"
          size="small"
          startIcon={<FullscreenExitIcon />}
          onClick={() => setFullscreen(false)}
        >
          Salir
        </Button>
      ) : (
        <Button
          color="success"
          size="small"
          startIcon={<FullscreenIcon />}
          onClick={() => setFullscreen(true)}
        >
          Pantalla completa
        </Button>
      )}
    </>
  );

  return (
    <>
      {!fullscreen && (
        <div className="relation-graph">
          <div className="relation-graph-toolbar">
            <div className="relation-graph-legend">
              {isMongo ? (
                <>
                  <span
                    className="relation-graph-legend__item"
                    title="Composición uno a uno"
                  >
                    <span className="relation-graph-legend__line relation-graph-legend__line--composition" />
                    1:1
                  </span>
                  <span
                    className="relation-graph-legend__item"
                    title="Composición uno a muchos"
                  >
                    <span className="relation-graph-legend__line relation-graph-legend__line--composition-list" />
                    1:N
                  </span>
                  <span className="relation-graph-legend__hint">
                    Click en una entidad para editarla · Arrastra desde el +
                    de una entidad raíz hacia un documento embebido · Click en
                    una arista para editarla.
                  </span>
                </>
              ) : (
                <>
                  <span
                    className="relation-graph-legend__item"
                    title="Muchos a uno"
                  >
                    <span className="relation-graph-legend__line" />
                    N:1
                  </span>
                  <span
                    className="relation-graph-legend__item"
                    title="Uno a muchos"
                  >
                    <span className="relation-graph-legend__line relation-graph-legend__line--dashed" />
                    1:N
                  </span>
                  <span
                    className="relation-graph-legend__item"
                    title="Uno a uno"
                  >
                    <span className="relation-graph-legend__line relation-graph-legend__line--one-to-one" />
                    1:1
                  </span>
                  <span
                    className="relation-graph-legend__item"
                    title="Muchos a muchos"
                  >
                    <span className="relation-graph-legend__line relation-graph-legend__line--many-to-many" />
                    N:M
                  </span>
                  <span className="relation-graph-legend__hint">
                    Click en una entidad para editarla en el panel · Arrastra
                    desde el + para crear una relación · Click en una arista
                    para editarla.
                  </span>
                </>
              )}
            </div>
            <div className="relation-graph-actions">{actions}</div>
          </div>
          {canvas}
        </div>
      )}

      {fullscreen &&
        createPortal(
          <div
            className="relation-graph-fs"
            role="dialog"
            aria-modal="true"
            aria-label="Mapa de vínculos"
          >
            <header className="relation-graph-fs__header">
              <div className="relation-graph-fs__titles">
                <span className="relation-graph-fs__eyebrow">
                  {isMongo ? "MAPA DE COMPOSICIONES" : "MAPA DE RELACIONES"}
                </span>
                <span className="relation-graph-fs__title">
                  Entidades y sus vínculos
                </span>
              </div>
              <div className="relation-graph-actions">
                {actions}
                <Tooltip title="Cerrar (Esc)">
                  <IconButton
                    aria-label="Cerrar mapa de relaciones"
                    className="relation-graph-fs__close"
                    size="small"
                    onClick={() => setFullscreen(false)}
                  >
                    <CloseIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </div>
            </header>
            <div className="relation-graph-fs__body">{canvas}</div>
          </div>,
          document.body,
        )}

      <RelationDialog
        open={!!dialog}
        mode={dialog?.mode || "create"}
        sourceName={dialog?.sourceName || ""}
        database={database}
        targetName={dialog?.targetName || ""}
        targetExists={dialog?.targetExists ?? true}
        targetEmbedded={dialog?.targetEmbedded ?? false}
        initialName={dialog?.initialName || ""}
        initialType={dialog?.initialType || "CUSTOM"}
        otherNames={dialog?.otherNames || []}
        suggestName={(type) => {
          const source = dialog ? sourcesById.get(dialog.sourceId) : null;
          return source
            ? suggestRelationName(source, dialog.targetName, type)
            : "";
        }}
        onClose={closeDialog}
        onSave={handleSaveRelation}
        onDelete={handleDeleteRelation}
      />

      <Snackbar
        open={!!notice}
        autoHideDuration={4500}
        onClose={() => setNotice(null)}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
        sx={{ zIndex: 1700 }}
      >
        <Alert
          severity={notice?.severity || "info"}
          variant="filled"
          onClose={() => setNotice(null)}
          sx={{ width: "100%" }}
        >
          {notice?.message}
        </Alert>
      </Snackbar>
    </>
  );
}

function RelationshipGraph({
  entities,
  database = "POSTGRESQL",
  selectedEntityUuid,
  onSelectEntity,
  onAddEntity,
  onCreateRelation,
  onUpdateRelation,
  onDeleteRelation,
}) {
  return (
    <ReactFlowProvider>
      <RelationGraphShell
        entities={entities}
        database={database}
        selectedEntityUuid={selectedEntityUuid}
        onSelectEntity={onSelectEntity}
        onAddEntity={onAddEntity}
        onCreateRelation={onCreateRelation}
        onUpdateRelation={onUpdateRelation}
        onDeleteRelation={onDeleteRelation}
      />
    </ReactFlowProvider>
  );
}

export function RelationshipGraphPanel({
  entities,
  relationCount,
  database = "POSTGRESQL",
  selectedEntityUuid,
  onSelectEntity,
  onAddEntity,
  onCreateRelation,
  onUpdateRelation,
  onDeleteRelation,
}) {
  const [open, setOpen] = useState(true);

  const toggle = () => setOpen((current) => !current);

  const entityCount = entities.length;
  const linkKind =
    database === "MONGODB"
      ? relationCount === 1
        ? "composición"
        : "composiciones"
      : relationCount === 1
        ? "relación"
        : "relaciones";
  const summary = `${entityCount} ${entityCount === 1 ? "entidad" : "entidades"} · ${relationCount} ${linkKind}`;

  return (
    <div className="relation-panel-container">
      <div className="relation-panel">
        <div
          className="relation-panel__header"
          role="button"
          tabIndex={0}
          aria-expanded={open}
          aria-controls="relation-panel-body"
          onClick={toggle}
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === " ") {
              event.preventDefault();
              toggle();
            }
          }}
        >
          <AccountTreeIcon className="relation-panel__icon" />
          <div className="relation-panel__titles">
            <Typography className="relation-panel__title">
              {database === "MONGODB"
                ? "Mapa de composiciones"
                : "Mapa de relaciones"}
            </Typography>
            <Typography className="relation-panel__subtitle">
              {summary}
            </Typography>
          </div>
          <ExpandMoreIcon
            className={`relation-panel__chevron${
              open ? " relation-panel__chevron--open" : ""
            }`}
          />
        </div>
        <Collapse in={open} timeout="auto" unmountOnExit>
          <div id="relation-panel-body" className="relation-panel__body">
            <RelationshipGraph
              entities={entities}
              database={database}
              selectedEntityUuid={selectedEntityUuid}
              onSelectEntity={onSelectEntity}
              onAddEntity={onAddEntity}
              onCreateRelation={onCreateRelation}
              onUpdateRelation={onUpdateRelation}
              onDeleteRelation={onDeleteRelation}
            />
          </div>
        </Collapse>
      </div>
    </div>
  );
}

export default RelationshipGraph;
