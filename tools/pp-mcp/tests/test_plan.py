import pytest

from pp_mcp.plan import Lookups, Missing, prepare_args, resolve

LOOKUPS = Lookups(
    cash={"Broker": "cash-broker", "Savings": "cash-savings"},
    investment={"Broker": "inv-broker", "Depot": "inv-depot"},
    by_isin={"CH0012005267": "inst-novartis"},
    by_name={"Novartis N": "inst-novartis"},
)


def test_unique_account_names_resolve_directly():
    assert resolve("@acct:Savings", LOOKUPS, "cash_account") == "cash-savings"
    assert resolve("@acct:Depot", LOOKUPS, "investment_account") == "inv-depot"


def test_shared_name_is_decided_by_the_parameter():
    assert resolve("@acct:Broker", LOOKUPS, "cash_account") == "cash-broker"
    assert resolve("@acct:Broker", LOOKUPS, "from_cash_account") == "cash-broker"
    assert resolve("@acct:Broker", LOOKUPS, "investment_account") == "inv-broker"


def test_shared_name_as_uuid_is_decided_by_the_tool():
    assert resolve("@acct:Broker", LOOKUPS, "uuid", "update_cash_account") == "cash-broker"
    assert resolve("@acct:Broker", LOOKUPS, "uuid", "update_investment_account") == "inv-broker"


def test_shared_name_without_hint_is_an_error():
    with pytest.raises(ValueError, match="@cash: or @inv:"):
        resolve("@acct:Broker", LOOKUPS, "account")


def test_explicit_prefixes_and_instruments():
    assert resolve("@cash:Broker", LOOKUPS) == "cash-broker"
    assert resolve("@inv:Broker", LOOKUPS) == "inv-broker"
    assert resolve("@isin:CH0012005267", LOOKUPS) == "inst-novartis"
    assert resolve("@inst:Novartis N", LOOKUPS) == "inst-novartis"


def test_nested_values_and_plain_strings():
    args = {"cash_account": "@acct:Broker", "instruments": ["@isin:CH0012005267"], "note": "@ home", "amount": "1.00"}
    assert resolve(args, LOOKUPS) == {
        "cash_account": "cash-broker",
        "instruments": ["inst-novartis"],
        "note": "@ home",
        "amount": "1.00",
    }


def test_unknown_entity_is_missing():
    with pytest.raises(Missing):
        resolve("@acct:Nowhere", LOOKUPS, "cash_account")


def test_unknown_placeholder_kind_is_an_error():
    with pytest.raises(ValueError, match="unknown placeholder"):
        resolve("@foo:bar", LOOKUPS)


@pytest.mark.parametrize("tool", ["create_buy", "commit_import", "import_pdf", "import_csv", "update_quotes"])
def test_dry_run_forces_dry_run_on_write_tools(tool):
    args = prepare_args(tool, {"dry_run": False, "x": 1}, "f", commit=False)
    assert args == {"file": "f", "dry_run": True, "x": 1}


@pytest.mark.parametrize("tool", ["create_buy", "commit_import", "import_pdf"])
def test_commit_applies_write_tools(tool):
    assert prepare_args(tool, {"dry_run": True}, "f", commit=True) == {"file": "f", "dry_run": False}


@pytest.mark.parametrize("tool", ["save_file", "open_file"])
def test_dry_run_skips_tools_without_dry_run(tool):
    assert prepare_args(tool, {}, "f", commit=False) is None


def test_commit_saves_the_plan_file():
    assert prepare_args("save_file", {}, "f", commit=True) == {"file": "f"}


def test_open_file_gets_only_its_own_arguments():
    assert prepare_args("open_file", {"path": "/p.xml"}, "f", commit=True) == {"path": "/p.xml"}
    assert prepare_args("list_files", {}, "f", commit=False) == {}


def test_read_tools_get_no_dry_run():
    assert prepare_args("list_accounts", {}, "f", commit=False) == {"file": "f"}
