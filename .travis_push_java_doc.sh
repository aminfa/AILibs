#!/bin/sh

setup_git() {
  git config --global user.email "travis@travis-ci.org"
  git config --global user.name "Travis CI"
}

commit_website_files() {
  git remote add origin https://github.com/aminfa/AILibs.git
  git remote update
  git fetch 
  git checkout --track javadoc
  git add ./\*.html
  git add ./\*.css
  git add ./\*.js
  git add ./\*package-list
  git commit --message "Travis built Javadoc"
}

upload_files() {
  git remote add origin-up https://${GH_TOKEN}@github.com/aminfa/AILibs.git > /dev/null 2>&1
  git push --quiet --set-upstream origin-up javadoc 
}

setup_git
commit_website_files
upload_files