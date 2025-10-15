import { useRouteQuery } from "@vueuse/router";
import { computed } from "vue";
import type { LocationQueryValue } from "vue-router";

export type QueryParamValue = LocationQueryValue | LocationQueryValue[];

export function queryParamAsString(value?: QueryParamValue): string | undefined {
  if (typeof value === "object" && value !== null) value = value[0];
  return value ?? undefined;
}

export function useQueryParamAsString(name: string) {
  const raw = useRouteQuery(name);
  return computed({
    get() {
      return queryParamAsString(raw.value) ?? "";
    },
    set(value) {
      raw.value = value === "" ? undefined : value;
    },
  });
}

export function queryParamAsNonemptyString(value?: QueryParamValue): string | undefined {
  const s = queryParamAsString(value);
  if (s === "") return undefined;
  return s;
}

export function queryParamAsInt(value?: QueryParamValue): number | undefined {
  const s = queryParamAsString(value);
  if (s === undefined) return;
  const n = parseInt(s, 10);
  if (Number.isNaN(n)) return;
  return n;
}

export function useQueryParamAsInt(name: string, defaultValue: number, options?: { min?: number; max?: number }) {
  const { min = undefined, max = undefined } = options ?? {};
  const raw = useRouteQuery(name);
  return computed({
    get() {
      let value = queryParamAsInt(raw.value) ?? defaultValue;
      if (min !== undefined) value = Math.max(min, value);
      if (max !== undefined) value = Math.min(max, value);
      return value;
    },
    set(value) {
      if (min !== undefined) value = Math.max(min, value);
      if (max !== undefined) value = Math.min(max, value);
      raw.value = value === defaultValue ? undefined : value.toFixed();
    },
  });
}

export function queryParamAsBool(value?: QueryParamValue): boolean | undefined {
  const s = queryParamAsString(value);
  if (s === undefined) return undefined;
  return s !== "" && s !== "false" && s !== "no";
}

export function useQueryParamAsBool(name: string, defaultValue: boolean) {
  const raw = useRouteQuery(name);
  return computed({
    get() {
      return queryParamAsBool(raw.value) ?? defaultValue;
    },
    set(value) {
      raw.value = value === defaultValue ? undefined : String(value);
    },
  });
}

export function queryParamAsStringArray(value?: QueryParamValue): string[] {
  if (value === undefined || value === null) return [];
  if (typeof value !== "object") value = [value];
  return value.filter((it) => it !== null);
}

export function useQueryParamAsStringArray(name: string) {
  const raw = useRouteQuery(name);
  return computed({
    get() {
      return queryParamAsStringArray(raw.value);
    },
    set(values) {
      raw.value = values;
    },
  });
}

export function queryParamAsStringSet(value?: QueryParamValue): Set<string> {
  return new Set(queryParamAsStringArray(value));
}

export function useQueryParamAsStringSet(name: string) {
  const raw = useRouteQuery(name);
  return computed({
    get() {
      return queryParamAsStringSet(raw.value);
    },
    set(values) {
      raw.value = Array.from(values).sort();
    },
  });
}
