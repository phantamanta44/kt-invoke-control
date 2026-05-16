{ pkgs ? import <nixpkgs> {} }:

let
  zulu = pkgs.zulu;
  zulu8 = pkgs.zulu8;
in

pkgs.mkShell {
  nativeBuildInputs = [ zulu ];
  buildInputs = [ zulu8 ];

  "ORG_GRADLE_PROJECT_org.gradle.java.installations.auto-detect" = "false";
  "ORG_GRADLE_PROJECT_org.gradle.java.installations.auto-download" = "false";
  "ORG_GRADLE_PROJECT_org.gradle.java.installations.paths" = "${zulu},${zulu8}";
}
