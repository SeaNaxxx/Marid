# @marid-dev/plugin-api

Type definitions for Marid plugin extensions. The package has types only; it has no runtime code.

Marid 插件扩展代码用的类型包。只有类型，没有运行时代码。

## Install

```sh
npm install --save-dev @marid-dev/plugin-api
```

## Entries

| Import                              | Types                                                        |
| ----------------------------------- | ------------------------------------------------------------ |
| `@marid-dev/plugin-api`             | `ExtensionContext`, `Disposable`, scopes, views, error codes |
| `@marid-dev/plugin-api/manifest`    | The manifest `extension` key, slots, grants                  |
| `@marid-dev/plugin-api/sessions`    | Session projection, session action inputs and results        |
| `@marid-dev/plugin-api/protocol-v4` | Conversation rows, context composition, generation metrics   |

```ts
import type { ExtensionContext } from "@marid-dev/plugin-api";

export function activate(ctx: ExtensionContext): void {
  ctx.commands.register("hello", () => ctx.log.info("hello"));
}
```

Use `import type` only. The package has no JavaScript.

## Versions

The package version is the plugin API version of Marid. Set `extension.engines.marid` in the plugin manifest to the version you build against, for example `"^1.4.0"`.

- A new capability or a new optional field raises the minor version.
- A removed capability or a changed meaning raises the major version.
- Marid loads an extension only when its plugin API version satisfies `engines.marid`.

插件清单的 `extension.engines.marid` 写你构建时用的版本，例如 `"^1.4.0"`。Marid 的插件 API 版本满足这个范围时才加载扩展。
