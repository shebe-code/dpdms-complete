# Rebuilding the GitHub workspace

This folder is the clean replacement for the earlier incomplete scaffold.

Recommended Windows procedure:

```powershell
cd C:\Users\ankin
Rename-Item dpdms dpdms-old
Expand-Archive C:\path\to\dpdms-complete.zip -DestinationPath C:\Users\ankin
Rename-Item dpdms-complete dpdms
cd C:\Users\ankin\dpdms
git init
git remote add origin https://github.com/B-hash122/dpdms.git
git fetch origin
git checkout -b complete-dpdms-build
git add .
git commit -m "Build complete DPDMS assignment solution"
git push -u origin complete-dpdms-build
```

If Git says the remote branch already exists locally, use:

```powershell
git checkout -b complete-dpdms-build
```

and continue from `git add .`.

Run `mvn clean test` before pushing and use GitHub Actions as the second verification layer.
