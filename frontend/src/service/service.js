import { saveAs } from "file-saver";

const formatStringToCamelCase = (val) => {
  let words = val.replaceAll("-", " ").split(" ");
  let newVal = words.map((w, indx) =>
    indx !== 0 ? w[0].toUpperCase() + w.substring(1) : w,
  );
  return newVal.join("");
};

const createRequestBody = (data) => {
  const entities = data.entities.map((e) => {
    let attributes = {};
    e.attributes.forEach((a) => {
      if (a.name) {
        attributes = {
          ...attributes,
          [formatStringToCamelCase(a.name)]: {
            ...a,
            name: formatStringToCamelCase(a.name),
            customType: a.customType
              ? formatStringToCamelCase(a.customType)
              : "",
          },
        };
      }
    });
    return { entityName: formatStringToCamelCase(e.entityName), attributes };
  });
  return { title: formatStringToCamelCase(data.businessName), entities };
};

const service = {
  createProject: async (data) => {
    const response = await fetch(`/api/domain`, {
      method: "POST",
      body: JSON.stringify(createRequestBody(data)),
      headers: { "Content-Type": "application/json" },
    });
    if (!response.ok) {
      const { error } = await response.json();
      return error;
    }
    const blob = await response.blob();
    saveAs(blob, formatStringToCamelCase(data.businessName) + ".zip");
  },
};

export default service;
