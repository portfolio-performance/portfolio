# Vendored ResourceBundle Editor Code

The classes in this package were taken over from the ResourceBundle Editor
project by Pascal Essiembre:

- https://github.com/essiembre/eclipse-rbe

They are used here in a reduced, self-contained form to read and write Java
`.properties` resource bundles in the same canonical format as the editor.

This copy was necessary because the original ResourceBundle Editor codebase is
not directly usable in this standalone Maven build plugin. In the upstream
project, the resource bundle parsing and generation logic depends on Eclipse
workbench/runtime code, so it cannot simply be reused in a plain Maven reactor
module that must run without Eclipse.

Only the minimal subset needed for parsing and generating resource bundles was
copied and adapted here.
