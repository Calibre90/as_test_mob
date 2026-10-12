#!/usr/bin/env python3
"""Regression tests for optional FAQ in signed Studio snapshot payloads."""
import importlib.util
import pathlib
import unittest

path=pathlib.Path(__file__).with_name("validate_faq_snapshot.py")
spec=importlib.util.spec_from_file_location("faq_validation",path)
module=importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

def snapshot(entries=None):
    import json
    stores={"run35_settings":{},"studio_admin_rows":{},"studio_custom_modules":{}}
    if entries is not None:
        stores["studio_faq"]={"entries":json.dumps(entries,ensure_ascii=False)}
    return {"schema":2,"kind":"mazda6gh-admin-settings","stores":stores}

class FaqTests(unittest.TestCase):
    def test_legacy_without_faq(self):
        self.assertEqual(module.validate(snapshot()),0)
    def test_valid_cyrillic(self):
        self.assertEqual(module.validate(snapshot([{"name":"RVM","description":"Контроль слепых зон"},{"name":"AFS","description":"Адаптивный свет"}])),2)
    def test_blank_title(self):
        with self.assertRaises(ValueError):module.validate(snapshot([{"name":" ","description":"x"}]))
    def test_oversized_description(self):
        with self.assertRaises(ValueError):module.validate(snapshot([{"name":"RVM","description":"x"*12001}]))
    def test_too_many_entries(self):
        with self.assertRaises(ValueError):module.validate(snapshot([{"name":"A","description":""}]*201))
    def test_unexpected_field(self):
        with self.assertRaises(ValueError):module.validate(snapshot([{"name":"RVM","description":"","secret":"x"}]))
    def test_non_list(self):
        doc=snapshot([]);doc["stores"]["studio_faq"]["entries"]="{}"
        with self.assertRaises(ValueError):module.validate(doc)

if __name__=="__main__":unittest.main()
